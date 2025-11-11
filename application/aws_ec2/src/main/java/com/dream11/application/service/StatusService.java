package com.dream11.application.service;

import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.constant.LoadBalancerType;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StatusService {
  @NonNull final DeployConfig deployConfig;
  @NonNull final RoutingService routingService;
  @NonNull final LoadBalancerService loadBalancerService;
  @NonNull final ClassicLoadBalancerService classicLoadBalancerService;

  public void getStatus() {
    if (this.deployConfig.getDiscoveryConfig().getType() == DiscoveryType.NONE) {
      // TODO healthcheck for non discoverable applications
      log.info("Skipping health checks as application is non discoverable");
    } else {
      this.getDiscoverableStatus();
    }
  }

  private void getDiscoverableStatus() {
    Map<String, Character> activeStackMap = this.routingService.getActiveStackMap(true);
    log.info(
        "Running healthcheck for deployed application on Active Stack map:[{}]", activeStackMap);

    List<String> unhealthyLbOrTgList =
        activeStackMap.entrySet().stream()
            .flatMap(
                entry -> this.getUnhealthyLoadbalancers(entry.getKey(), entry.getValue()).stream())
            .toList();
    if (!unhealthyLbOrTgList.isEmpty()) {
      throw new GenericApplicationException(
          ApplicationError.UNHEALTHY_INSTANCES, unhealthyLbOrTgList);
    }
  }

  private List<String> getUnhealthyLoadbalancers(String stackId, Character deploymentStack) {
    if (this.deployConfig.getLoadBalancerConfig().getType() == LoadBalancerType.CLB) {
      return this.getUnhealthyLoadbalancers(
          stackId,
          deploymentStack,
          this.classicLoadBalancerService::getLoadBalancerNamesForStack,
          this.classicLoadBalancerService::getHealthyInstances);
    } else {
      return this.getUnhealthyLoadbalancers(
          stackId,
          deploymentStack,
          this.loadBalancerService::getTargetGroupArnsForStack,
          this.loadBalancerService::getHealthyTargets);
    }
  }

  private List<String> getUnhealthyLoadbalancers(
      String stackId,
      Character deploymentStack,
      BiFunction<String, Character, List<String>> getLbOrTgFn,
      ToLongFunction<String> getHealthTargetFn) {
    return getLbOrTgFn.apply(stackId, deploymentStack).stream()
        .filter(lbOrTg -> getHealthTargetFn.applyAsLong(lbOrTg) < 1)
        .toList();
  }
}
