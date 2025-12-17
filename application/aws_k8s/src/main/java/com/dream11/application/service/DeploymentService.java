package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.config.ComponentMetadata;
import com.dream11.application.config.DeployConfig;
import com.dream11.application.config.user.RollingRestartConfig;
import com.dream11.application.constant.CanaryPhase;
import com.dream11.application.constant.Constants;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class DeploymentService {

  @NonNull final KubernetesService kubernetesService;
  @NonNull final HelmService helmService;
  @NonNull final ComponentMetadata componentMetadata;
  @NonNull final DeployConfig deployConfig;
  @NonNull final StatusService statusService;

  public void scale() {
    String deploymentName =
        Application.getState().getDeploymentState().getHelmReleaseName() + "-primary";
    String namespace = Application.getState().getDeploymentNamespace();
    int targetReplicas = this.deployConfig.getReplicas();

    log.info(
        "Scaling deployment: {} in namespace: {} to {} replicas",
        deploymentName,
        namespace,
        targetReplicas);

    this.kubernetesService.scaleDeployment(deploymentName, namespace, targetReplicas);
    this.waitForHealthyReplicas(deploymentName, targetReplicas);
  }

  public void deploy() {
    log.info(
        "Starting deploy operation for component: {}", this.componentMetadata.getComponentName());

    this.helmService.deployApplication();

    log.info(
        "Successfully completed deploy for component: {}",
        this.componentMetadata.getComponentName());
  }

  public void redeploy() {
    log.info(
        "Starting redeploy operation for component: {}", this.componentMetadata.getComponentName());

    this.helmService.deployApplication();

    String componentName = this.componentMetadata.getComponentName();
    String namespace = Application.getState().getDeploymentNamespace();

    Optional<CanaryPhase> canaryStatus =
        this.kubernetesService.getCanaryStatus(componentName, namespace);
    if (canaryStatus.isPresent()) {
      log.info("Canary resource found for component: {}, checking status", componentName);
      Optional<CanaryPhase> finalPhase =
          this.kubernetesService.waitForCanaryCompletion(componentName, namespace);

      if (finalPhase.isPresent() && finalPhase.get() == CanaryPhase.FAILED) {
        throw new GenericApplicationException(
            ApplicationError.HEALTHY_PODS_COUNT_LESS_THAN_ONE,
            "Canary deployment failed for component: " + componentName);
      } else if (finalPhase.isPresent() && finalPhase.get() == CanaryPhase.SUCCEEDED) {
        log.info("Canary succeeded for component: {}", componentName);
      } else {
        log.warn(
            "Canary did not reach Succeeded or Failed state for component: {}, final phase: {}",
            componentName,
            finalPhase.map(CanaryPhase::getValue).orElse("unknown"));
      }
    } else {
      log.info(
          "No Canary resource found for component: {}, skipping Canary status check",
          componentName);
    }

    log.info(
        "Successfully completed redeploy for component: {}",
        this.componentMetadata.getComponentName());
  }

  public void revert() {
    String componentName = Application.getState().getDeploymentState().getHelmReleaseName();
    String namespace = Application.getState().getDeploymentNamespace();

    log.info(
        "Starting revert operation for component: {} in namespace: {}", componentName, namespace);

    // Use helm rollback to revert to previous release
    this.helmService.rollback(componentName, namespace);

    log.info("Successfully completed revert for component: {}", componentName);
  }

  public void rollingRestart(RollingRestartConfig rollingRestartConfig) {
    String deploymentName =
        Application.getState().getDeploymentState().getHelmReleaseName() + "-primary";
    String namespace = Application.getState().getDeploymentNamespace();

    log.info(
        "Starting rolling restart for deployment: {} in namespace: {}", deploymentName, namespace);

    this.kubernetesService.restartDeployment(
        deploymentName, namespace, rollingRestartConfig.getBatchSizePercentage());

    log.info("Successfully completed rolling restart for deployment: {}", deploymentName);
  }

  @SneakyThrows
  public boolean waitForHealthyTargets(
      Callable<Boolean> getHealthyTargetCountMapper, Duration timeout) {
    log.info("Waiting for healthy targets");
    long startTime = System.nanoTime();
    while (System.nanoTime() <= startTime + timeout.toNanos()) {
      if (getHealthyTargetCountMapper.call()) {
        return true;
      }
      Thread.sleep(1000);
    }
    return false;
  }

  public void waitForHealthyReplicas(String releaseName, int desiredCount) {
    boolean healthy =
        this.waitForHealthyTargets(
            () -> this.statusService.getStatus(desiredCount),
            Constants.WAIT_FOR_INITIAL_HEALHTY_TARGETS_DURATION);

    if (!healthy) {
      throw new GenericApplicationException(
          ApplicationError.HEALTHY_PODS_COUNT_LESS_THAN_ONE, releaseName);
    }
  }
}
