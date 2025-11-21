package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.exception.HelmReleaseNotFoundException;
import com.dream11.application.state.State;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StateCorrectionService {

  @NonNull final HelmService helmService;

  public void correctState() {
    State state = Application.getState();
    this.correctDeploymentState(state);
  }

  private void correctDeploymentState(State state) {
    if (state.getDeploymentState() == null) {
      log.info("No deployment states found. Skipping deployment state correction.");
      return;
    }
    try {
      this.helmService.getReleaseStatus(
          state.getDeploymentState().getHelmReleaseName(),
          Application.getState().getDeploymentNamespace());
    } catch (HelmReleaseNotFoundException ex) {
      log.warn(
          "Helm release:[{}] from state does not exist. Updating state.",
          state.getDeploymentState().getHelmReleaseName());
      state.setDeploymentState(null);
      return;
    }
  }
}
