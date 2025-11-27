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

  @Override
  public boolean execute() {
    log.info("Starting undeploy process");
    this.helmService.uninstallAllReleases();
    Application.getState().setDeploymentNamespace(null);
    Application.getState().setSecretExist(false);
    Application.getState().setImage(null);
    log.info("Undeploy process completed successfully");
    return true;
  }
}
