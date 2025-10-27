package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.config.user.UpdateStackConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class UpdateStackService {
  @NonNull final RoutingService routingService;
  @NonNull final DeploymentService deploymentService;
  @NonNull final AutoscalingGroupService autoscalingGroupService;
  @NonNull final InfrastructureService infrastructureService;
  @NonNull final UpdateStackConfig updateStackConfig;
  @NonNull final DeployConfig deployConfig;
  final Integer previousStackNumber = Application.getState().getDeployConfig().getStacks();

  public void updateStack() {
    if (this.previousStackNumber >= this.deployConfig.getStacks()) {
      throw new GenericApplicationException(
          ApplicationError.INVALID_STACK_NUMBER,
          this.deployConfig.getStacks(),
          this.previousStackNumber);
    }
    if (this.deployConfig.getDiscoveryConfig().getType() == DiscoveryType.NONE) {
      throw new GenericApplicationException(
          ApplicationError.OPERATION_NOT_ALLOWED_FOR_NON_DISCOVERABLE_APPLICATION, "update-stack");
    }
    this.infrastructureService.createInfrastructure();

    Map<String, Character> activeStackMap =
        this.routingService.getActiveStackMap(
            IntStream.range(1, this.previousStackNumber + 1).mapToObj(String::valueOf).toList());
    Map<String, Character> newPassiveStackMap =
        this.routingService.getPassiveStackMap(
            IntStream.range(this.previousStackNumber + 1, this.deployConfig.getStacks() + 1)
                .mapToObj(String::valueOf)
                .toList());
    Pair<List<AutoScalingGroup>, Integer> asgDetails =
        this.createNewAsgs(activeStackMap, newPassiveStackMap);
    List<Callable<Boolean>> tasks =
        new ArrayList<>(
            deploymentService.createAllAsgWaitTasks(
                asgDetails.getLeft(),
                asgDetails.getRight(),
                Constants.WAIT_FOR_TOTAL_HEALHTY_INSTANCES_DURATION));

    List<Pair<String, String>> lbDetails =
        this.scaleLoadBalancers(activeStackMap, newPassiveStackMap);
    tasks.addAll(this.deploymentService.createAllLcuWaitTasks(lbDetails));
    //
    ApplicationUtil.runOnExecutorService(tasks);
    if (this.updateStackConfig.getAutoRouting().equals(Boolean.TRUE)) {
      routingService.routeFullTraffic(
          ApplicationUtil.merge(List.of(activeStackMap, newPassiveStackMap)));
    } else {
      log.warn("Skipping routing.");
    }
  }

  private Pair<List<AutoScalingGroup>, Integer> createNewAsgs(
      Map<String, Character> currentActiveStackMap, Map<String, Character> newPassiveStackMap) {

    String uniqueId = ApplicationUtil.generateRandomId(Constants.ASG_RANDOM_ID_LENGTH);
    List<AutoScalingGroup> createdAsgs =
        newPassiveStackMap.entrySet().stream()
            .map(
                entry ->
                    this.deploymentService.createLtAndAsgForDiscoverable(
                        uniqueId, entry.getKey(), entry.getValue()))
            .toList();
    List<AutoScalingGroup> currentAsgs =
        currentActiveStackMap.entrySet().stream()
            .map(
                entry ->
                    autoscalingGroupService.describe(
                        this.deploymentService
                            .getAsgsForStack(entry.getKey(), entry.getValue())
                            .get(0)))
            .toList();
    int totalDesiredCapacity =
        currentAsgs.stream().mapToInt(AutoScalingGroup::desiredCapacity).sum();
    List<AutoScalingGroup> allAsgs = new ArrayList<>();
    allAsgs.addAll(currentAsgs);
    allAsgs.addAll(createdAsgs);
    Integer instanceCountPerAsg = deploymentService.scaleAsgEqually(allAsgs, totalDesiredCapacity);
    return Pair.of(allAsgs, instanceCountPerAsg);
  }

  private List<Pair<String, String>> scaleLoadBalancers(
      Map<String, Character> activeStackMap, Map<String, Character> newPassiveStackMap) {
    String stackId = activeStackMap.entrySet().iterator().next().getKey();
    Character deploymentStack = activeStackMap.entrySet().iterator().next().getValue();
    Map<Character, Integer> lcuCount =
        this.deploymentService.getCurrentLcusForStack(stackId, deploymentStack);
    return newPassiveStackMap.entrySet().stream()
        .flatMap(
            entry ->
                this.deploymentService
                    .scaleLcus(entry.getKey(), entry.getValue(), lcuCount)
                    .stream())
        .toList();
  }
}
