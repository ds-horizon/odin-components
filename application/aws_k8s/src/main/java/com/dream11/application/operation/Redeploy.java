package com.dream11.application.operation;

import com.dream11.application.service.DeploymentService;
import com.dream11.application.service.StateCorrectionService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Redeploy implements Operation {

  @NonNull final DeploymentService deploymentService;
  @NonNull final StateCorrectionService stateCorrectionService;

  @Override
  public boolean execute() {
    log.info("Starting redeploy operation");
    this.stateCorrectionService.correctState();
    this.deploymentService.redeploy();
    log.info("Redeploy operation completed successfully");
    return true;
  }
}
