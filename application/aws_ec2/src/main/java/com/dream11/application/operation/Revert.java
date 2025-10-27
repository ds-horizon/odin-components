package com.dream11.application.operation;

import com.dream11.application.config.user.RevertConfig;
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
  @NonNull final RevertConfig revertConfig;
  @NonNull final StateCorrectionService stateCorrectionService;

  @Override
  public boolean execute() {
    this.stateCorrectionService.correctState();
    this.deploymentService.revert(this.revertConfig);
    return false;
  }
}
