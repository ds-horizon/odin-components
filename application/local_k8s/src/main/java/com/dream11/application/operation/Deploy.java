package com.dream11.application.operation;

import com.dream11.application.Application;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.constant.Constants;
import com.dream11.application.service.HelmService;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Deploy implements Operation {

  @NonNull final HelmService helmService;

  @NonNull final ComponentMetadata componentMetadata;

  public boolean execute() {
    if (Objects.isNull(Application.getState().getReleaseName())) {
      Application.getState()
          .setReleaseName(
              this.componentMetadata.getComponentName()
                  + "-"
                  + ApplicationUtil.generateRandomId(Constants.UUID_LENGTH).toLowerCase());
    }
    if (Objects.isNull(Application.getState().getDeploymentNamespace())) {
      Application.getState()
          .setDeploymentNamespace(this.componentMetadata.getDeploymentNamespace());
    }
    this.helmService.upgrade(
        Application.getState().getReleaseName(), Application.getState().getDeploymentNamespace());
    log.info("Deployment completed successfully");
    return true;
  }
}
