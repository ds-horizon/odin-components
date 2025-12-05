package com.dream11.application.operation;

import com.dream11.application.Application;
import com.dream11.application.config.ComponentMetadata;
import com.dream11.application.config.DeployConfig;
import com.dream11.application.service.DeploymentService;
import com.dream11.application.service.KubernetesService;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Deploy implements Operation {

  final KubernetesService kubernetesService;
  final DeploymentService deploymentService;
  final ComponentMetadata componentMetadata;
  final DeployConfig deployConfig;

  @Override
  public boolean execute() {
    log.info("Starting deployment process");

    // Ensure namespace and secrets are configured
    this.kubernetesService.configureDeploymentNamespace();
    this.deploymentService.deploy();
    this.kubernetesService.waitForCanaryInitialized(
        this.componentMetadata.getComponentName(), Application.getState().getDeploymentNamespace());
    this.deploymentService.waitForHealthyReplicas(
        this.componentMetadata.getComponentName() + "-primary", this.deployConfig.getReplicas());
    return true;
  }
}
