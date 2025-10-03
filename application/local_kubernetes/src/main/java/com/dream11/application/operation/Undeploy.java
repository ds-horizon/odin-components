package com.dream11.application.operation;

import com.dream11.application.Application;
import com.dream11.application.service.HelmService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Undeploy implements Operation {

  @NonNull final HelmService helmService;

  public boolean execute() {
    this.helmService.uninstall(
        Application.getState().getReleaseName(), Application.getState().getDeploymentNamespace());
    Application.getState().clearReleaseName();
    return true;
  }
}
