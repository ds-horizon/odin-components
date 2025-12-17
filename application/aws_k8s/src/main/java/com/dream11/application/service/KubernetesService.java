package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.client.HelmClient;
import com.dream11.application.client.K8sClient;
import com.dream11.application.config.ComponentMetadata;
import com.dream11.application.config.DeployConfig;
import com.dream11.application.config.aws.AwsAccountData;
import com.dream11.application.config.aws.DockerRegistryData;
import com.dream11.application.config.aws.EKSData;
import com.dream11.application.config.aws.VPCData;
import com.dream11.application.constant.CanaryPhase;
import com.dream11.application.entity.DockerConfig;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.util.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class KubernetesService {

  private static final String GATEWAY_CHART_RELEASE_NAME = "gateway";
  private static final String GATEWAY_CHART_PATH = "./gateway-chart";

  @NonNull final K8sClient k8sClient;
  @NonNull final HelmClient helmClient;
  @NonNull final ComponentMetadata componentMetadata;
  @NonNull final AwsAccountData awsAccountData;
  @NonNull final DockerRegistryData dockerRegistryData;
  @NonNull final EKSData eksData;
  @NonNull final VPCData vpcData;
  @NonNull final ObjectMapper objectMapper;
  @NonNull final DeployConfig deployConfig;

  public void configureDeploymentNamespace() {
    this.createDeploymentNamespace(this.componentMetadata.getDeploymentNamespace());
    this.createDeploymentSecret(
        this.eksData.getPullSecrets(), Application.getState().getDeploymentNamespace());
  }

  private void createDeploymentNamespace(String namespace) {
    if (Objects.isNull(Application.getState().getDeploymentNamespace())) {
      // Create namespace if not exist
      this.k8sClient
          .getNamespace(namespace)
          .ifPresentOrElse(
              __ -> log.info("Namespace:[{}] found", namespace),
              () -> {
                log.info("Creating namespace:[{}]", namespace);
                this.k8sClient.createNamespace(namespace, this.awsAccountData.getTags(), Map.of());
                this.installGatewayChart(namespace);
              });
      // Namespace created, update state
      Application.getState().setDeploymentNamespace(namespace);
    } else {
      log.info("Namespace:[{}] found", namespace);
    }
  }

  private void createDeploymentSecret(List<String> names, String namespace) {
    if (Application.getState().isSecretExist()) {
      log.info("Docker registry secret already exists, skipping creation");
      return;
    }
    // Create docker secret if not exist
    names.forEach(
        name ->
            this.k8sClient
                .getSecret(name, namespace)
                .ifPresentOrElse(
                    __ -> log.info("Secret:[{}] found", name),
                    () -> {
                      log.info("Creating secret:[{}]", name);
                      this.createDockerConfigSecret(name, namespace);
                    }));
    // Secret created, update state
    Application.getState().setSecretExist(true);
  }

  @SneakyThrows
  private void createDockerConfigSecret(String name, String namespace) {
    DockerConfig dockerConfig =
        DockerConfig.builder()
            .auths(
                Map.of(
                    this.dockerRegistryData.getRegistry(),
                    DockerConfig.Auth.builder()
                        .username(this.dockerRegistryData.getUsername())
                        .password(this.dockerRegistryData.getPassword())
                        .build()))
            .build();
    String dockerConfigJson = this.objectMapper.writeValueAsString(dockerConfig);
    this.k8sClient.createSecret(
        name,
        "kubernetes.io/dockerconfigjson",
        Map.of(
            ".dockerconfigjson", Base64.getEncoder().encodeToString(dockerConfigJson.getBytes())),
        namespace);
  }

  private void installGatewayChart(String namespace) {
    log.info("Installing gateway chart in namespace:[{}]", namespace);

    if (helmClient.releaseExists(GATEWAY_CHART_RELEASE_NAME, namespace)) {
      log.debug("Gateway chart already exists in namespace:[{}], skipping installation", namespace);
      return;
    }

    String gatewayValuesPath = this.generateGatewayValues();
    helmClient.upgradeInstall(
        GATEWAY_CHART_RELEASE_NAME, namespace, GATEWAY_CHART_PATH, gatewayValuesPath);
    log.info("Successfully installed gateway chart in namespace:[{}]", namespace);
  }

  @SneakyThrows
  public String generateGatewayValues() {
    log.info("Generating gateway values file");
    try (InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("helm/gateway-values.yaml")) {
      if (Objects.isNull(inputStream)) {
        throw new GenericApplicationException(ApplicationError.GATEWAY_CHART_VALUES_PATH_NOT_FOUND);
      }
      String content = IOUtils.toString(inputStream, Charset.defaultCharset());
      String substitutedValues =
          ApplicationUtil.substituteValues(
              "gateway-values", content, this.buildGatewayTemplateData());
      String valuesPath = Paths.get("generated-gateway-values.yaml").toAbsolutePath().toString();
      this.createGatewayValuesFile(substitutedValues, valuesPath);
      log.debug("Gateway values file generated at path: {}", valuesPath);
      log.debug("Gateway values file content: \n{}", substitutedValues);
      return valuesPath;
    }
  }

  @SneakyThrows
  private void createGatewayValuesFile(String templateContent, String valuesFile) {
    FileUtils.writeStringToFile(new File(valuesFile), templateContent, Charset.defaultCharset());
  }

  private Map<String, Object> buildGatewayTemplateData() {
    return Map.of(
        "vpc_data", this.vpcData,
        "service_name", this.componentMetadata.getComponentName(),
        "environment_name", this.componentMetadata.getDeploymentNamespace(),
        "component_name", this.componentMetadata.getComponentName());
  }

  public long getReadyReplicaCount(String deploymentName, String namespace) {
    return this.k8sClient.getReadyReplicaCount(deploymentName, namespace);
  }

  public void scaleDeployment(String deploymentName, String namespace, int replicas) {
    this.k8sClient.scaleDeployment(deploymentName, namespace, replicas);
  }

  @SneakyThrows
  public void restartDeployment(String deploymentName, String namespace, int batchSizePercentage) {
    log.info(
        "Restarting deployment: {} in namespace: {} with batch size: {}%",
        deploymentName, namespace, batchSizePercentage);

    this.k8sClient.restartDeployment(deploymentName, namespace, batchSizePercentage, Map.of());
  }

  public Optional<CanaryPhase> getCanaryStatus(String canaryName, String namespace) {
    return this.k8sClient.getCanaryPhase(canaryName, namespace);
  }

  @SneakyThrows
  public Optional<CanaryPhase> waitForCanaryInitialized(String canaryName, String namespace) {
    Duration timeout = Duration.ofMinutes(10);
    log.debug(
        "Waiting for Canary to reach Initialized state: {} in namespace: {} with timeout: {}",
        canaryName,
        namespace,
        timeout);
    long startTime = System.nanoTime();
    CanaryPhase lastPhase = null;

    while (System.nanoTime() <= startTime + timeout.toNanos()) {
      Optional<CanaryPhase> phaseOpt = this.getCanaryStatus(canaryName, namespace);
      if (phaseOpt.isPresent()) {
        CanaryPhase phase = phaseOpt.get();
        if (!phase.equals(lastPhase)) {
          log.debug("Canary {} phase: {}", canaryName, phase.getValue());
          lastPhase = phase;
        }

        if (phase == CanaryPhase.INITIALIZED) {
          log.debug("Canary {} reached Initialized state", canaryName);
          return Optional.of(phase);
        }
      }

      Thread.sleep(1000);
    }

    log.warn(
        "Timeout waiting for Canary to reach Initialized state: {} in namespace: {}",
        canaryName,
        namespace);
    return Optional.ofNullable(lastPhase);
  }

  @SneakyThrows
  public Optional<CanaryPhase> waitForCanaryCompletion(String canaryName, String namespace) {
    log.info(
        "Waiting for Canary completion: {} in namespace: {}, checking every minute for up to 20 minutes",
        canaryName,
        namespace);
    Duration timeout = Duration.ofMinutes(20);
    long startTime = System.nanoTime();
    CanaryPhase lastPhase = null;

    while (System.nanoTime() <= startTime + timeout.toNanos()) {
      Optional<CanaryPhase> phase = this.getCanaryStatus(canaryName, namespace);
      if (phase.isPresent()) {
        if (!phase.equals(lastPhase)) {
          log.debug("Canary {} phase: {}", canaryName, phase.get().getValue());
          lastPhase = phase.get();
        }

        if (phase.get() == CanaryPhase.SUCCEEDED && lastPhase != null) {
          log.info("Canary {} succeeded", canaryName);
          return Optional.of(phase.get());
        }

        if (phase.get() == CanaryPhase.FAILED && lastPhase != null) {
          log.error("Canary {} failed", canaryName);
          return Optional.of(phase.get());
        }
      }

      Thread.sleep(Duration.ofMinutes(1).toMillis());
    }

    log.warn(
        "Timeout waiting for Canary completion: {} in namespace: {} after 20 minutes",
        canaryName,
        namespace);
    return Optional.ofNullable(lastPhase);
  }
}
