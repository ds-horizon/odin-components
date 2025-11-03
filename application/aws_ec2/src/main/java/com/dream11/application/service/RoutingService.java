package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.config.user.BlueGreenStrategyConfig;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.ErrorMetric;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.util.ApplicationUtil;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RoutingService {

  @NonNull final DeployConfig deployConfig;

  @NonNull final Route53Service route53Service;

  @NonNull final LoadBalancerService loadBalancerService;
  @NonNull final ClassicLoadBalancerService classicLoadBalancerService;

  public void routeTraffic(
      BlueGreenStrategyConfig config, Map<String, Character> deploymentStackMap) {
    if (config.getCanaryConfig().getEnabled().equals(Boolean.TRUE)) {
      this.routeTrafficWithCanary(config, deploymentStackMap);
    } else {
      // Route traffic in one shot
      log.info("Routing full traffic as canary is not enabled");
      this.routeFullTraffic(deploymentStackMap);
    }
  }

  private void routeTrafficWithCanary(
      BlueGreenStrategyConfig config, Map<String, Character> deploymentStackMap) {
    // Shift weight in steps and then wait for canary analysis
    Map<String, Long> weightDistribution =
        ApplicationUtil.getWeightDistribution(this.deployConfig.getStacks());

    // Current weight map. After each step the weights will be updated
    Map<String, Map<Character, Map<Character, Long>>> expandedWeightMap =
        this.route53Service.getExpandedRoute53Weights();
    Map<String, Map<Character, Long>> currentWeights =
        expandedWeightMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry ->
                        expandedWeightMap.get(entry.getKey()).values().stream()
                            .toList()
                            .get(0))); // Guaranteed to have one element

    // Create copy of current weights, will be used for revering if canary analysis fails
    Map<String, Map<Character, Long>> initialWeights =
        currentWeights.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    log.info("Current route53 weights:[{}]", currentWeights);
    try {
      for (int i = 0; i < config.getCanaryConfig().getSteps().getCount(); i++) {
        // Canary analysis iteration
        log.info(
            "Starting canary analysis step:[{}] for seconds:[{}]",
            i + 1,
            config.getCanaryConfig().getSteps().getDuration());
        deploymentStackMap.forEach(
            (stackId, deploymentStack) -> {
              Long weightShift =
                  weightDistribution.get(stackId)
                      * config.getCanaryConfig().getSteps().getWeight()
                      / 100;
              Character sisterDeploymentStack =
                  ApplicationUtil.getSisterDeploymentStack(deploymentStack);
              Long deploymentStackWeight =
                  currentWeights.get(stackId).get(deploymentStack) + weightShift;
              Long sisterDeploymentStackWeight =
                  Math.max(
                      0L, currentWeights.get(stackId).get(sisterDeploymentStack) - weightShift);
              Map<Character, Long> weightsToUpdate =
                  Map.of(
                      deploymentStack,
                      deploymentStackWeight,
                      sisterDeploymentStack,
                      sisterDeploymentStackWeight);
              this.route53Service.setWeights(stackId, weightsToUpdate);
              // Update current weights
              currentWeights.get(stackId).put(deploymentStack, deploymentStackWeight);
              currentWeights.get(stackId).put(sisterDeploymentStack, sisterDeploymentStackWeight);
            });
        log.info(
            "Waiting [{}s] to allow the metrics to stabilize",
            Constants.DELAY_FOR_CLOUD_METRICS_STABILIZATION.toSeconds());
        List<Callable<Boolean>> tasks =
            this.createAllCanaryAnalysisTasks(config, deploymentStackMap);
        ApplicationUtil.runOnExecutorService(tasks);
        log.info("Canary analysis step:[{}] completed successfully", i + 1);
      }
      // Shift to 100% and perform canary analysis
      log.info(
          "Starting canary analysis with full traffic for [{}] seconds",
          config.getCanaryConfig().getSteps().getDuration());
      this.routeFullTraffic(deploymentStackMap);
      log.info(
          "Waiting [{}s] to allow the metrics to stabilize",
          Constants.DELAY_FOR_CLOUD_METRICS_STABILIZATION.toSeconds());
      List<Callable<Boolean>> tasks = this.createAllCanaryAnalysisTasks(config, deploymentStackMap);
      ApplicationUtil.runOnExecutorService(tasks);
      log.info("Canary analysis completed successfully");

    } catch (Exception ex) {
      // Revert weights
      log.error("Canary analysis failed, reverting route53 weights");
      deploymentStackMap.forEach(
          (stackId, deploymentStack) ->
              this.route53Service.setWeights(stackId, initialWeights.get(stackId)));
      throw ex;
    }
  }

  private List<Callable<Boolean>> createAllCanaryAnalysisTasks(
      BlueGreenStrategyConfig config, Map<String, Character> deploymentStackMap) {
    List<Callable<Boolean>> tasks = new ArrayList<>();
    deploymentStackMap.forEach(
        (stackId, deploymentStack) -> {
          tasks.addAll(
              this.createCanaryAnalysisTasks(
                  config, stackId, deploymentStack, Constants.INTERNAL_IDENTIFIER));
          tasks.addAll(
              this.createCanaryAnalysisTasks(
                  config, stackId, deploymentStack, Constants.EXTERNAL_IDENTIFIER));
        });
    return tasks;
  }

  private List<Callable<Boolean>> createCanaryAnalysisTasks(
      BlueGreenStrategyConfig config, String stackId, Character deploymentStack, Character type) {
    List<Callable<Boolean>> tasks = new ArrayList<>();
    Duration canaryDuration = Duration.ofSeconds(config.getCanaryConfig().getSteps().getDuration());
    Pair<String, String> lbInfo =
        Application.getState()
            .getLbArnOrName(String.format("%s%s%s", stackId, type, deploymentStack));

    if (Objects.nonNull(lbInfo.getLeft())) {
      // Application/Network load balancers
      tasks.add(
          () ->
              this.doCanaryAnalysis(
                  config,
                  instant -> this.loadBalancerService.getRequestCount(lbInfo.getLeft(), instant),
                  instant -> this.loadBalancerService.getErrorCount(lbInfo.getLeft(), instant),
                  canaryDuration));
    }
    if (Objects.nonNull(lbInfo.getRight())) {
      // Classic load balancer
      tasks.add(
          () ->
              this.doCanaryAnalysis(
                  config,
                  instant ->
                      this.classicLoadBalancerService.getRequestCount(lbInfo.getRight(), instant),
                  instant ->
                      this.classicLoadBalancerService.getErrorCount(lbInfo.getRight(), instant),
                  canaryDuration));
    }
    return tasks;
  }

  @SneakyThrows
  private boolean doCanaryAnalysis(
      BlueGreenStrategyConfig config,
      ToDoubleFunction<Instant> requestCountFn,
      ToDoubleFunction<Instant> errorCountFn,
      Duration duration) {
    long startTime = System.currentTimeMillis();
    Instant startInstant = Instant.ofEpochMilli(startTime);
    Thread.sleep(Constants.DELAY_FOR_CLOUD_METRICS_STABILIZATION.toMillis());
    while (System.currentTimeMillis()
        <= startTime
            + duration.toMillis()
            + Constants.DELAY_FOR_CLOUD_METRICS_STABILIZATION.toMillis()) {
      Double requestCount = requestCountFn.applyAsDouble(startInstant);
      Double errorCount = errorCountFn.applyAsDouble(startInstant);
      this.verifyCanaryMetrics(requestCount, errorCount, config);
      Thread.sleep(Constants.DELAY_FOR_MAKING_NEXT_REQUEST.toMillis());
    }
    return true;
  }

  private void verifyCanaryMetrics(
      Double requestCount, Double errorCount, BlueGreenStrategyConfig config) {
    Integer threshold = config.getCanaryConfig().getErrorThreshold().getValue();
    if (config.getCanaryConfig().getErrorThreshold().getMetric() == ErrorMetric.ABSOLUTE) {
      if (errorCount > threshold) {
        throw new GenericApplicationException(
            ApplicationError.ABSOLUTE_CANARY_ANALYSIS_FAILED, errorCount, threshold);
      }
    } else {
      if (requestCount == 0) {
        return;
      }
      double errorPercentage = errorCount / requestCount * 100;
      if (errorPercentage > threshold) {
        throw new GenericApplicationException(
            ApplicationError.PERCENTAGE_CANARY_ANALYSIS_FAILED, errorPercentage, threshold);
      }
    }
  }

  public void routeFullTraffic(Map<String, Character> deploymentStackMap) {
    Map<String, Long> weightDistribution =
        ApplicationUtil.getWeightDistribution(this.deployConfig.getStacks());
    deploymentStackMap.forEach(
        (stackId, deploymentStack) -> {
          Map<Character, Long> weightsToUpdate =
              Map.of(
                  deploymentStack,
                  weightDistribution.get(stackId),
                  ApplicationUtil.getSisterDeploymentStack(deploymentStack),
                  0L);
          this.route53Service.setWeights(stackId, weightsToUpdate);
        });
  }

  public Map<String, Character> getPassiveStackMap() {
    return this.getPassiveStackMap(null);
  }

  /**
   * Validates if the weights of route53 records are valid for performing deployment Following
   * validations are performed
   *
   * <p>For each stack 0 weight routes should be consistent for both internal (if exists) and
   * external (if exists) For each stack there should be at least 1 route with zero weight
   *
   * @param stackIds optional containing stackIds to filter
   * @return Map<String, Character> map for passive stacks. Example: {0 -> b, 1 -> g}
   */
  public Map<String, Character> getPassiveStackMap(List<String> stackIds) {
    Map<String, Character> passiveStackMap = new HashMap<>();
    Map<String, Map<Character, Map<Character, Long>>> expandedWeightkMap =
        this.route53Service.getFilteredRoute53Weights(stackIds);
    log.info("Route53 weight map:[{}]", expandedWeightkMap);
    Multiset<Long> weightDistributionSet =
        HashMultiset.create(
            ApplicationUtil.getWeightDistribution(this.deployConfig.getStacks()).values());
    weightDistributionSet.add(0L);
    expandedWeightkMap.forEach(
        (stackId, typeWeightMap) -> {
          Set<String> zeroWeightIdentifiers = new HashSet<>();
          Set<Long> nonZeroWeights = new HashSet<>();
          typeWeightMap.forEach(
              (type, weightMap) -> {
                StringBuilder zeroWeights = new StringBuilder();
                for (Character stackIdentifier :
                    List.of(
                        Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER,
                        Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER)) {
                  Long weight =
                      weightMap.computeIfAbsent(
                          stackIdentifier,
                          k -> {
                            throw new GenericApplicationException(
                                ApplicationError.ROUTE53_NOT_FOUND, stackId, type, stackIdentifier);
                          });
                  if (weight == 0) {
                    zeroWeights.append(stackIdentifier);
                  } else if (!weightDistributionSet.contains(weight)) {
                    throw new GenericApplicationException(
                        ApplicationError.INVALID_ROUTE53_WEIGHT,
                        weight,
                        stackId,
                        type,
                        stackIdentifier,
                        weightDistributionSet.elementSet());
                  } else {
                    nonZeroWeights.add(weight);
                  }
                }
                zeroWeightIdentifiers.add(zeroWeights.toString());
              });

          if (zeroWeightIdentifiers.size() != 1 || nonZeroWeights.size() > 1) {
            throw new GenericApplicationException(ApplicationError.INCONSISTENT_ROUTE53, stackId);
          }
          String zeroWeightIdentifier = zeroWeightIdentifiers.iterator().next();
          if (zeroWeightIdentifier.isEmpty()) {
            throw new GenericApplicationException(
                ApplicationError.NO_PASSIVE_RECORD_FOUND, stackId);
          }
          nonZeroWeights.iterator().forEachRemaining(weightDistributionSet::remove);
          passiveStackMap.put(stackId, zeroWeightIdentifier.charAt(0));
        });
    return passiveStackMap;
  }

  public Map<String, Character> getActiveStackMap() {
    return this.getActiveStackMap(null, false);
  }

  public Map<String, Character> getActiveStackMap(List<String> stackIds) {
    return this.getActiveStackMap(stackIds, false);
  }

  public Map<String, Character> getActiveStackMap(boolean allowZeroWeights) {
    return this.getActiveStackMap(null, allowZeroWeights);
  }

  public Map<String, Character> getActiveStackMap(List<String> stackIds, boolean allowZeroWeights) {
    Map<String, Character> activeStack = new HashMap<>();
    Map<String, Map<Character, Map<Character, Long>>> expandedWeightkMap =
        this.route53Service.getFilteredRoute53Weights(stackIds);
    log.info("Route53 weight map:[{}]", expandedWeightkMap);

    expandedWeightkMap.forEach(
        (stackId, typeWeightMap) -> {
          Set<String> nonZeroWeightIdentifiers = new HashSet<>();
          typeWeightMap.forEach(
              (type, weightMap) -> {
                StringBuilder nonZeroWeights = new StringBuilder();
                Long blueWeight =
                    weightMap.computeIfAbsent(
                        Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER,
                        k -> {
                          throw new GenericApplicationException(
                              ApplicationError.ROUTE53_NOT_FOUND,
                              stackId,
                              type,
                              Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER);
                        });
                if (blueWeight != 0) {
                  nonZeroWeights.append(Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER);
                }
                Long greenWeight =
                    weightMap.computeIfAbsent(
                        Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER,
                        k -> {
                          throw new GenericApplicationException(
                              ApplicationError.ROUTE53_NOT_FOUND,
                              stackId,
                              type,
                              Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER);
                        });
                if (greenWeight != 0) {
                  nonZeroWeights.append(Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER);
                }
                nonZeroWeightIdentifiers.add(nonZeroWeights.toString());
              });

          if (nonZeroWeightIdentifiers.size() != 1) {
            throw new GenericApplicationException(ApplicationError.INCONSISTENT_ROUTE53, stackId);
          }
          String nonZeroWeightIdentifier = nonZeroWeightIdentifiers.iterator().next();
          if (allowZeroWeights && nonZeroWeightIdentifier.isEmpty()) {
            log.warn(String.format("Route53s for stackId:[%s] are having zero weight.", stackId));
            return;
          }
          if (nonZeroWeightIdentifier.length() != 1) {
            throw new GenericApplicationException(
                ApplicationError.MULTIPLE_ACTIVE_RECORDS, stackId);
          }
          activeStack.put(stackId, nonZeroWeightIdentifier.charAt(0));
        });
    return activeStack;
  }
}
