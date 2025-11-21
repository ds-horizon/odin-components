package com.dream11.application.operation;

import com.dream11.application.Application;
import com.dream11.application.constant.Constants;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.service.DeploymentService;
import com.dream11.application.service.StateCorrectionService;
import com.dream11.application.service.StatusService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Status implements Operation {
  @NonNull final StatusService statusService;
  @NonNull final DeploymentService deploymentService;
  @NonNull final StateCorrectionService stateCorrectionService;

  public boolean execute() {
    this.stateCorrectionService.correctState();

    boolean healthy =
        this.deploymentService.waitForHealthyTargets(
            () -> this.statusService.getStatus(1), Constants.WAIT_FOR_HEALTHY_TARGETS_DURATION);

    if (!healthy) {
      throw new GenericApplicationException(
          ApplicationError.HEALTHY_PODS_COUNT_LESS_THAN_ONE,
          Application.getState().getDeploymentState().getHelmReleaseName());
    }
    return true;
  }
}
