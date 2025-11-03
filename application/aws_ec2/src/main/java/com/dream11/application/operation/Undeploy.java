package com.dream11.application.operation;

import com.dream11.application.service.AutoscalingGroupService;
import com.dream11.application.service.ClassicLoadBalancerService;
import com.dream11.application.service.LaunchTemplateService;
import com.dream11.application.service.LoadBalancerService;
import com.dream11.application.service.Route53Service;
import com.dream11.application.service.StateCorrectionService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Undeploy implements Operation {

  @NonNull final LoadBalancerService loadBalancerService;
  @NonNull final ClassicLoadBalancerService classicLoadBalancerService;
  @NonNull final Route53Service route53Service;
  @NonNull final LaunchTemplateService launchTemplateService;
  @NonNull final AutoscalingGroupService autoscalingGroupService;
  @NonNull final StateCorrectionService stateCorrectionService;

  @Override
  public boolean execute() {
    this.stateCorrectionService.correctState();
    this.autoscalingGroupService.deleteAsgs();
    this.launchTemplateService.deleteLaunchTemplates();
    this.classicLoadBalancerService.deleteClassicLoadBalancers();
    this.loadBalancerService.deleteListeners();
    this.loadBalancerService.deleteTargetGroups();
    this.loadBalancerService.deleteLoadBalancers();
    this.route53Service.deleteRoute53s();
    return true;
  }
}
