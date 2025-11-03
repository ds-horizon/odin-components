package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.aws.ClassicLoadBalancerClient;
import com.dream11.application.aws.CloudwatchClient;
import com.dream11.application.config.metadata.aws.AwsAccountData;
import com.dream11.application.config.metadata.aws.DiscoveryData;
import com.dream11.application.config.metadata.aws.EC2Data;
import com.dream11.application.config.metadata.aws.NetworkData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.config.user.LoadBalancerConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.entity.CloudWatchMetric;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.state.LoadBalancerState;
import com.dream11.application.state.State;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerSchemeEnum;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ClassicLoadBalancerService {
  @NonNull final ClassicLoadBalancerClient classicLoadBalancerClient;
  @NonNull final CloudwatchClient cloudwatchClient;
  @NonNull final NetworkData networkData;
  @NonNull final DeployConfig deployConfig;
  @NonNull final EC2Data ec2Data;
  @NonNull final DiscoveryData discoveryData;
  @NonNull final AwsAccountData awsAccountData;

  Map<String, String> tags;

  @Inject
  private void init() {
    this.tags =
        ApplicationUtil.merge(
            List.of(
                this.awsAccountData.getTags(),
                this.ec2Data.getTags(),
                this.deployConfig.getTags(),
                Constants.COMPONENT_TAGS));
  }

  private Optional<LoadBalancerDescription> getClassicLoadBalancerFromState(
      String identifier, LoadBalancerState lbState, Runnable stateCorrectionFn) {
    // Check if load balancer actually exist
    try {
      LoadBalancerDescription loadBalancerDescription =
          this.classicLoadBalancerClient.describe(lbState.getName());
      log.info("Load balancer:[{}] for identifier:[{}] found", lbState.getName(), identifier);
      return Optional.of(loadBalancerDescription);
    } catch (
        software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerNotFoundException
            loadBalancerNotFoundException) {
      log.warn(
          "Load balancer from state:[{}] does not exists. Correcting state...", lbState.getName());
      stateCorrectionFn.run();
    }
    return Optional.empty();
  }

  public void createClassicLoadBalancerAndListener(String identifier, Character type) {
    Optional<LoadBalancerState> lbState = Application.getState().getLoadBalancerState(identifier);
    if (lbState.isEmpty()
        || this.getClassicLoadBalancerFromState(
                identifier,
                lbState.get(),
                () -> Application.getState().removeLoadBalancerState(identifier))
            .isEmpty()) {
      // Validate listener
      this.validateListenersConfig();
      // Create new load balancer and add to state
      String id = ApplicationUtil.generateRandomId(Constants.LB_RANDOM_ID_LENGTH);
      LoadBalancerDescription loadBalancer = null;
      if (type.equals(Constants.INTERNAL_IDENTIFIER)) {
        String certificateArn =
            ApplicationUtil.getCertificateArn(
                discoveryData, this.deployConfig.getDiscoveryConfig().getPrivateRoute());
        loadBalancer =
            this.classicLoadBalancerClient.create(
                String.format("%s-%s", id, identifier),
                LoadBalancerSchemeEnum.INTERNAL.toString(),
                this.networkData.getLbSecurityGroups().getInternal(),
                this.networkData.getLbSubnets().getPrivateSubnets(),
                this.deployConfig.getLoadBalancerConfig().getListeners(),
                certificateArn,
                this.tags);
      } else if (type.equals(Constants.EXTERNAL_IDENTIFIER)) {
        String certificateArn =
            ApplicationUtil.getCertificateArn(
                discoveryData, this.deployConfig.getDiscoveryConfig().getPublicRoute());
        loadBalancer =
            this.classicLoadBalancerClient.create(
                String.format("%s-%s", id, identifier),
                LoadBalancerSchemeEnum.INTERNET_FACING.toString(),
                this.networkData.getLbSecurityGroups().getExternal(),
                this.networkData.getLbSubnets().getPublicSubnets(),
                this.deployConfig.getLoadBalancerConfig().getListeners(),
                certificateArn,
                this.tags);
      }
      Objects.requireNonNull(loadBalancer);
      log.info(
          "Load balancer:[{}] created for identifier:[{}]",
          loadBalancer.loadBalancerName(),
          identifier);
      log.info(
          "Configuring health checks for classic load balancer:[{}]",
          loadBalancer.loadBalancerName());
      Application.getState()
          .addLoadBalancerState(
              loadBalancer, identifier, this.deployConfig.getLoadBalancerConfig().getListeners());
    }
  }

  private void validateListenersConfig() {
    if (this.deployConfig.getLoadBalancerConfig().getListeners().stream()
            .map(LoadBalancerConfig.Listener::getHealthChecks)
            .collect(Collectors.toSet())
            .size()
        != 1) {
      throw new GenericApplicationException(ApplicationError.INVALID_HEALTHCHECK_CONFIG);
    }
  }

  public void deleteClassicLoadBalancers() {
    log.info("Deleting classic load balancers");
    State state = Application.getState();
    List.copyOf(state.getLb())
        .forEach(
            loadBalancerState -> {
              if (Objects.isNull(loadBalancerState.getArn())) {
                log.debug("Deleting load balancer:[{}]", loadBalancerState.getName());
                this.classicLoadBalancerClient.delete(loadBalancerState.getName());
                log.info("Deleted load balancer:[{}]", loadBalancerState.getName());
                state.removeLoadBalancerState(loadBalancerState.getRouteIdentifier());
              }
            });
  }

  public long getHealthyInstances(String loadBalancerName) {
    long healthyInstances =
        this.classicLoadBalancerClient.describeInstanceHealth(loadBalancerName).stream()
            .filter(
                instanceState -> instanceState.state().equals(Constants.CLB_HEALTHY_INSTANCE_STATE))
            .count();
    log.debug(
        "Number of healthy instances in load balancer:[{}] is {}",
        loadBalancerName,
        healthyInstances);
    return healthyInstances;
  }

  public long getAllInstances(String loadBalancerName) {
    long instances = this.classicLoadBalancerClient.describeInstanceHealth(loadBalancerName).size();
    log.debug("Number of instances in load balancer:[{}] is {}", loadBalancerName, instances);
    return instances;
  }

  private Double getMetrics(String loadBalancerName, String metric, Instant startTime) {
    CloudWatchMetric cloudWatchMetric =
        CloudWatchMetric.builder()
            .resourceName("LoadBalancerName")
            .resourceValue(loadBalancerName)
            .metricName(metric)
            .namespace("AWS/ELB")
            .build();
    return ApplicationUtil.sumList(
        this.cloudwatchClient
            .getMetric(
                startTime, Instant.ofEpochMilli(System.currentTimeMillis() + 1), cloudWatchMetric)
            .get(0)
            .values());
  }

  public Double getRequestCount(String loadBalancerArn, Instant startTime) {
    Double requestCount = this.getMetrics(loadBalancerArn, "RequestCount", startTime);
    log.debug("Request count:[{}] for load balancer:[{}]", requestCount, loadBalancerArn);
    return requestCount;
  }

  public Double getErrorCount(String loadBalancerArn, Instant startTime) {
    Double errorCount =
        this.getMetrics(loadBalancerArn, "HTTPCode_Backend_5XX", startTime)
            + this.getMetrics(loadBalancerArn, "HTTPCode_ELB_5XX", startTime);
    log.debug("Error count:[{}] for load balancer:[{}]", errorCount, loadBalancerArn);
    return errorCount;
  }

  public List<String> getLoadBalancerNamesForStack(String stackId, Character deploymentStack) {
    Optional<LoadBalancerState> internal =
        Application.getState()
            .getLoadBalancerState(
                String.format("%s%s%s", stackId, Constants.INTERNAL_IDENTIFIER, deploymentStack));
    Optional<LoadBalancerState> internetFacing =
        Application.getState()
            .getLoadBalancerState(
                String.format("%s%s%s", stackId, Constants.EXTERNAL_IDENTIFIER, deploymentStack));
    List<String> loadBalancerNames = new ArrayList<>();
    internal.ifPresent(loadBalancerState -> loadBalancerNames.add(loadBalancerState.getName()));
    internetFacing.ifPresent(
        loadBalancerState -> loadBalancerNames.add(loadBalancerState.getName()));
    return loadBalancerNames;
  }
}
