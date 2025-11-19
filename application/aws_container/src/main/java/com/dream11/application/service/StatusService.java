package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.config.DeployConfig;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StatusService {
  final DeployConfig deployConfig;
  final KubernetesService kubernetesService;

  public boolean getStatus(long numberOfReadyTargets) {
    return this.kubernetesService.getReadyReplicaCount(
            Application.getState().getDeploymentState().getHelmReleaseName() + "-primary",
            Application.getState().getDeploymentNamespace())
        >= numberOfReadyTargets;
  }
}
