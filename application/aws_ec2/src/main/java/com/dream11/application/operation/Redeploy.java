package com.dream11.application.operation;

import com.dream11.application.service.DeploymentService;
import com.dream11.application.service.InfrastructureService;
import com.dream11.application.service.StateCorrectionService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Redeploy implements Operation {
  @NonNull final DeploymentService deploymentService;
  @NonNull final InfrastructureService infrastructureService;
  @NonNull final StateCorrectionService stateCorrectionService;

  public boolean execute() {
    this.stateCorrectionService.correctState();
    this.infrastructureService.createInfrastructure();
    this.deploymentService.deploy();
    return true;
  }
}
