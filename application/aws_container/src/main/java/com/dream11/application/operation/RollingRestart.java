package com.dream11.application.operation;

import com.dream11.application.config.user.RollingRestartConfig;
import com.dream11.application.service.DeploymentService;
import com.dream11.application.service.StateCorrectionService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RollingRestart implements Operation {

  @NonNull final DeploymentService deploymentService;
  @NonNull final StateCorrectionService stateCorrectionService;
  @NonNull final RollingRestartConfig rollingRestartConfig;

  @Override
  public boolean execute() {
    log.info("Starting rolling restart operation");
    this.stateCorrectionService.correctState();
    this.deploymentService.rollingRestart(this.rollingRestartConfig);
    log.info("Rolling restart operation completed successfully");
    return true;
  }
}
