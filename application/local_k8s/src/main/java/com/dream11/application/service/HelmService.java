package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.client.HelmClient;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.config.metadata.local.DockerRegistryData;
import com.dream11.application.config.metadata.local.KubernetesData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.exception.HelmReleaseNotFoundException;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
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

  @NonNull final DeployConfig deployConfig;

  @NonNull final DockerRegistryData dockerRegistryData;

  @NonNull final ComponentMetadata componentMetadata;

  @NonNull final KubernetesData kubernetesData;

  @SneakyThrows
  private String generateHelmValues() {
    try (InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(Constants.HELM_VALUES_FILE)) {
      if (Objects.isNull(inputStream)) {
        throw new GenericApplicationException(
            ApplicationError.HELM_TEMPLATE_FILE_NOT_FOUND, Constants.HELM_VALUES_FILE);
      }
      String content = IOUtils.toString(inputStream, Charset.defaultCharset());
      return ApplicationUtil.substituteValues("values", content, this.buildTemplateData());
    }
  }

  private Map<String, Object> buildTemplateData() {
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
                    Constants.APPLICATION_DIRECTORY.apply(
                        this.deployConfig.getArtifactConfig().getName()),
                    "DEPLOYMENT_TYPE",
                    Constants.DEPLOYMENT_TYPE),
                this.kubernetesData.getEnvironmentVariables(),
                this.deployConfig.getExtraEnvVars()));
    return Map.of(
        "environmentVariables", environmentVariables,
        "registry", this.dockerRegistryData.getRegistry(),
        "deployConfig", this.deployConfig,
        "pullSecrets", this.kubernetesData.getPullSecrets(),
        "tag", Application.getState().getImage().getTag());
  }

  /**
   * Install helm chart
   *
   * @param releaseName Helm release name
   * @param namespace Kubernetes namespace for helm release
   */
  public void upgrade(String releaseName, String namespace) {
    // Install/Upgrade the application
    this.createValuesFile(this.generateHelmValues());
    log.debug("Upgrading helm release:{} in namespace:{}", releaseName, namespace);
    this.helmClient.upgrade(
        releaseName,
        namespace,
        "chart/",
        Constants.VALUES_FILE,
        Constants.WAIT_FOR_HEALHTY_TARGETS_DURATION.toMinutes() + "m");
    log.info("Helm release:{} in namespace:{} upgraded successfully", releaseName, namespace);
  }

  public void uninstall(String releaseName, String namespace) {
    log.debug("Uninstalling helm release:[{}]", releaseName);
    try {
      this.helmClient.uninstall(releaseName, namespace);
    } catch (HelmReleaseNotFoundException ex) {
      log.warn("Helm release:[{}] not found", releaseName);
      return;
    }
    log.info("Helm release:[{}] uninstalled successfully", releaseName);
  }

  @SneakyThrows
  private void createValuesFile(String templateContent) {
    FileUtils.writeStringToFile(
        new File(Constants.VALUES_FILE), templateContent, Charset.defaultCharset());
  }
}
