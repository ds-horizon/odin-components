package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.client.DockerClient;
import com.dream11.application.config.metadata.local.DockerRegistryData;
import com.dream11.application.config.metadata.local.KubernetesData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.config.user.ImageConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ImageService {
  @NonNull final DeployConfig deployConfig;
  @NonNull final DockerClient dockerClient;
  @NonNull final DockerRegistryData dockerRegistryData;
  @NonNull final KubernetesData kubernetesData;

  public void createImageIfNotExist() {
    String imageTag;
    if (!this.deployConfig.getLocalArtifact().isEnabled()) {
      // Non local artifact
      String artifactSha =
          Objects.requireNonNull(
              Application.getState().getImage().getArtifactSha(),
              "Invalid state, artifactSha in image state cannot be null");
      String idempotencySha =
          this.calculateIdempotencySha(artifactSha, this.deployConfig.getBaseImage());
      imageTag = this.deployConfig.getArtifactConfig().getVersion() + "-" + idempotencySha;
      if (this.dockerClient.checkIfImageExists(
          this.deployConfig.getArtifactConfig().getName(), imageTag)) {
        log.info(
            "Image for artifact:[{}@{}] with artifact SHA:[{}] and base image config:[{}] already exists",
            this.deployConfig.getArtifactConfig().getName(),
            this.deployConfig.getArtifactConfig().getVersion(),
            artifactSha,
            this.deployConfig.getBaseImage());
        Application.getState().getImage().setTag(imageTag);
        return;
      }
    } else {
      // Local artifact
      imageTag = "local";
    }
    log.info("Generating packer template for image creation");
    this.createPackerFile(this.generatePackerTemplate(imageTag));
    Application.getState().getImage().setTag(imageTag);
  }

  @SneakyThrows
  private void createPackerFile(String templateContent) {
    FileUtils.writeStringToFile(
        new File(Constants.PACKER_FILE), templateContent, Charset.defaultCharset());
  }

  @SneakyThrows
  private String generatePackerTemplate(String imageTag) {
    try (InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(Constants.PACKER_TEMPLATE_FILE)) {
      if (Objects.isNull(inputStream)) {
        throw new GenericApplicationException(
            ApplicationError.PACKER_TEMPLATE_FILE_NOT_FOUND, Constants.PACKER_TEMPLATE_FILE);
      }
      String content = IOUtils.toString(inputStream, Charset.defaultCharset());
      return ApplicationUtil.substituteValues("image", content, this.buildTemplateData(imageTag));
    }
  }

  @SneakyThrows
  private Map<String, Object> buildTemplateData(String imageTag) {
    String artifactName = this.deployConfig.getArtifactConfig().getName();

    return Map.ofEntries(
        Map.entry("artifact_name", artifactName),
        Map.entry("artifact_version", this.deployConfig.getArtifactConfig().getVersion()),
        Map.entry("registry", this.dockerRegistryData.getRegistry()),
        Map.entry("base_image_name", this.deployConfig.getBaseImage().getRepository()),
        Map.entry("base_image_tag", this.deployConfig.getBaseImage().getTag()),
        Map.entry("image_tag", imageTag),
        Map.entry(
            "setup_script",
            this.deployConfig.getArtifactConfig().getHooks().getImageSetup().getScript()),
        Map.entry(
            "setup_script_enabled",
            this.deployConfig.getArtifactConfig().getHooks().getImageSetup().isEnabled()),
        Map.entry(
            "start_script",
            this.deployConfig.getArtifactConfig().getHooks().getStart().getScript()),
        Map.entry("base_dir", Constants.BASE_DIR),
        Map.entry(
            "environment_variables",
            ApplicationUtil.merge(
                List.of(
                    Map.of(
                        "ARTIFACT_NAME",
                        artifactName,
                        "BASE_DIR",
                        Constants.BASE_DIR,
                        "APP_DIR",
                        Constants.APPLICATION_DIRECTORY.apply(artifactName),
                        "DEPLOYMENT_TYPE",
                        Constants.DEPLOYMENT_TYPE),
                    this.kubernetesData.getEnvironmentVariables(),
                    this.deployConfig.getExtraEnvVars()))));
  }

  public String calculateIdempotencySha(String artifactSha, ImageConfig config) {
    return DigestUtils.sha256Hex(artifactSha + config.getRepository() + config.getTag());
  }
}
