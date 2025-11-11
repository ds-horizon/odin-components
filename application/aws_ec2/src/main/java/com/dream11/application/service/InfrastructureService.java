package com.dream11.application.service;

import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.LoadBalancerType;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class InfrastructureService {
  @NonNull final DeployConfig deployConfig;
  @NonNull final LoadBalancerService loadBalancerService;
  @NonNull final ClassicLoadBalancerService classicLoadBalancerService;
  @NonNull final Route53Service route53Service;

  public void createInfrastructure() {
    log.info("Creating load balancers, target groups and route53s if they do not exist...");
    List<String> privateIdentifiers =
        ApplicationUtil.getPrivateIdentifiers(
            this.deployConfig.getStacks(), this.deployConfig.getDiscoveryConfig().getType());
    List<String> publicIdentifiers =
        ApplicationUtil.getPublicIdentifiers(
            this.deployConfig.getStacks(), this.deployConfig.getDiscoveryConfig().getType());
    privateIdentifiers.forEach(
        identifier -> this.createInfrastructure(identifier, Constants.INTERNAL_IDENTIFIER));
    publicIdentifiers.forEach(
        identifier -> this.createInfrastructure(identifier, Constants.EXTERNAL_IDENTIFIER));
  }

  public void createInfrastructure(String identifier, Character type) {
    log.info(
        "Creating load balancers, target groups and route53s if they do not exist for identifier:[{}]",
        identifier);
    if (this.deployConfig.getLoadBalancerConfig().getType() == LoadBalancerType.CLB) {
      this.classicLoadBalancerService.createClassicLoadBalancerAndListener(identifier, type);
    } else {
      this.loadBalancerService.createLoadBalancer(identifier, type);
      this.loadBalancerService.createTargetGroupsAndListeners(identifier, type);
    }
    this.route53Service.createRoute53(identifier, type);
  }
}
