package com.dream11.application.operation;

import com.dream11.application.service.KubernetesService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class CreateNamespace implements Operation {

  @NonNull final KubernetesService kubernetesService;

  @Override
  public boolean execute() {
    this.kubernetesService.configureDeploymentNamespace();
    return false;
  }
}
