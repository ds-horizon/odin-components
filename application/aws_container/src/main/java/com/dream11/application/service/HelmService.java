package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.client.HelmClient;
import com.dream11.application.config.ComponentMetadata;
import com.dream11.application.config.DeployConfig;
import com.dream11.application.config.aws.AwsAccountData;
import com.dream11.application.config.aws.DockerRegistryData;
import com.dream11.application.config.aws.EKSData;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.state.DeploymentState;
import com.dream11.application.util.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class HelmService {

  @NonNull final HelmClient helmClient;
  @NonNull final ComponentMetadata componentMetadata;
  @NonNull final AwsAccountData awsAccountData;
  @NonNull final DockerRegistryData dockerRegistryData;
  @NonNull final EKSData eksData;
  @NonNull final DeployConfig deployConfig;
  @NonNull final ImageService imageService;
  @NonNull final KubernetesService kubernetesService;
  @NonNull final ObjectMapper objectMapper;

  @SneakyThrows
  public void deployApplication() {
    String releaseName = this.componentMetadata.getComponentName();
    String namespace = Application.getState().getDeploymentNamespace();
    String chartPath = "./chart";

    // Generate Helm values file
    String valuesPath = this.generateHelmValues();

    try {
      helmClient.upgradeInstall(releaseName, namespace, chartPath, valuesPath);
    } finally {
      // Clean up temporary values file
      Application.getState()
          .setDeploymentState(DeploymentState.builder().helmReleaseName(releaseName).build());
      new File(valuesPath).delete();
    }
  }

  public void uninstallAllReleases() {
    if (Application.getState().getDeploymentState() == null) {
      log.info("No deployment states found. Skipping uninstall.");
      return;
    }
    this.uninstall(
        Application.getState().getDeploymentState().getHelmReleaseName(),
        Application.getState().getDeploymentNamespace());
  }

  public void uninstall(String releaseName, String namespace) {
    log.debug("Uninstalling helm release:[{}]", releaseName);
    this.helmClient.uninstall(releaseName, namespace);
    Application.getState().setDeploymentState(null);
    log.info("Helm release:[{}] uninstalled successfully", releaseName);
  }

  public void getReleaseStatus(String releaseName, String namespace) {
    if (!this.helmClient.releaseExists(releaseName, namespace)) {
      throw new com.dream11.application.exception.HelmReleaseNotFoundException(
          "Helm release not found: " + releaseName);
    }
  }

  @SneakyThrows
  private String generateHelmValues() {
    log.debug("Generating helm values file");
    try (InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(Constants.HELM_VALUES_FILE)) {
      if (Objects.isNull(inputStream)) {
        throw new GenericApplicationException(
            ApplicationError.HELM_TEMPLATE_FILE_NOT_FOUND, Constants.HELM_VALUES_FILE);
      }
      String content = IOUtils.toString(inputStream, Charset.defaultCharset());
      String substitutedValues =
          ApplicationUtil.substituteValues("values", content, this.buildTemplateData());
      String valuesPath = Paths.get("generated-values.yaml").toAbsolutePath().toString();
      this.createValuesFile(substitutedValues, valuesPath);
      log.debug("Helm values file generated at path: {}", valuesPath);
      log.debug("Helm values file content: \n{}", substitutedValues);
      return valuesPath;
    }
  }

  @SneakyThrows
  private void createValuesFile(String templateContent, String valuesFile) {
    FileUtils.writeStringToFile(new File(valuesFile), templateContent, Charset.defaultCharset());
  }

  private Map<String, Object> buildTemplateData() {
    String appDirectory =
        Constants.APPLICATION_DIRECTORY.apply(this.deployConfig.getArtifactConfig().getName());
    Map<String, String> labels =
        ApplicationUtil.merge(List.of(this.deployConfig.getTags(), Constants.COMPONENT_TAGS));
    Map<String, String> environmentVariables =
        ApplicationUtil.merge(
            List.of(
                Map.of(
                    "COMPONENT_NAME",
                    this.componentMetadata.getComponentName(),
                    "ARTIFACT_NAME",
                    this.deployConfig.getArtifactConfig().getName(),
                    "ARTIFACT_VERSION",
                    this.deployConfig.getArtifactConfig().getVersion(),
                    "APP_DIR",
                    appDirectory,
                    "DEPLOYMENT_TYPE",
                    Constants.DEPLOYMENT_TYPE,
                    "ODIN_CPU_LIMIT",
                    ApplicationUtil.parseCpu(this.deployConfig.getResources().getLimits().getCpu()),
                    "ODIN_CPU_REQUEST",
                    ApplicationUtil.parseCpu(
                        this.deployConfig.getResources().getRequests().getCpu()),
                    "ODIN_MEMORY_LIMIT",
                    ApplicationUtil.parseMemory(
                        this.deployConfig.getResources().getLimits().getMemory()),
                    "ODIN_MEMORY_REQUEST",
                    ApplicationUtil.parseMemory(
                        this.deployConfig.getResources().getRequests().getMemory())),
                this.eksData.getEnvironmentVariables(),
                this.deployConfig.getExtraEnvVars()));

    return Map.ofEntries(
        Map.entry("environmentVariables", environmentVariables),
        Map.entry("registry", this.dockerRegistryData.getRegistry()),
        Map.entry("deployConfig", this.deployConfig),
        Map.entry("tag", this.deployConfig.getArtifactConfig().getVersion()),
        Map.entry("pullSecrets", this.eksData.getPullSecrets()),
        Map.entry("serviceAnnotations", this.eksData.getServiceAnnotations()),
        Map.entry("serviceAccountAnnotations", this.eksData.getServiceAccountAnnotations()),
        Map.entry("serviceAccountName", this.componentMetadata.getComponentName()),
        Map.entry("componentName", this.componentMetadata.getComponentName()),
        Map.entry("labels", labels),
        Map.entry(
            "enableProbes", this.deployConfig.getDiscoveryConfig().getType() != DiscoveryType.NONE),
        Map.entry(
            "idempotencySha",
            this.imageService.calculateIdempotencySha(
                Application.getState().getImage().getArtifactSha(),
                this.deployConfig.getBaseImage())));
  }

  public void rollback(String releaseName, String namespace) {
    this.helmClient.rollback(releaseName, namespace);
  }
}
