package com.dream11.application.operation;

import com.dream11.application.service.DeploymentService;
import com.dream11.application.service.StateCorrectionService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Revert implements Operation {
  @NonNull final DeploymentService deploymentService;
  @NonNull final StateCorrectionService stateCorrectionService;

  @Override
  public boolean execute() {
    log.info("Starting revert operation");
    this.stateCorrectionService.correctState();
    this.deploymentService.revert();
    log.info("Revert operation completed successfully");
    return false;
  }
}
