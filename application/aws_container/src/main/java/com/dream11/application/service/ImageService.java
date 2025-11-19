package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.client.DockerClient;
import com.dream11.application.config.DeployConfig;
import com.dream11.application.config.ImageConfig;
import com.dream11.application.config.aws.DockerRegistryData;
import com.dream11.application.constant.Constants;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
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

  public void createImageIfNotExist() {
    String artifactSha =
        Objects.requireNonNull(
            Application.getState().getImage().getArtifactSha(),
            "Invalid state, artifactSha in image state cannot be null");
    String idempotencySha =
        this.calculateIdempotencySha(artifactSha, this.deployConfig.getBaseImage());
    Boolean imageExists =
        this.dockerClient.checkIfImageExists(
            this.deployConfig.getArtifactConfig().getName(),
            this.deployConfig.getArtifactConfig().getVersion() + "-" + idempotencySha);
    if (Boolean.TRUE.equals(imageExists)) {
      log.info(
          "Image for artifact:[{}@{}] with artifact SHA:[{}] and base image config:[{}] already exists",
          this.deployConfig.getArtifactConfig().getName(),
          this.deployConfig.getArtifactConfig().getVersion(),
          artifactSha,
          this.deployConfig.getBaseImage());
    } else {
      log.info("Generating packer template for image creation");
      this.createPackerFile(this.generatePackerTemplate(idempotencySha));
    }
  }

  @SneakyThrows
  private void createPackerFile(String templateContent) {
    FileUtils.writeStringToFile(
        new File(Constants.PACKER_FILE), templateContent, Charset.defaultCharset());
  }

  @SneakyThrows
  private String generatePackerTemplate(String idempotencySha) {
    try (InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(Constants.PACKER_TEMPLATE_FILE)) {
      if (Objects.isNull(inputStream)) {
        throw new GenericApplicationException(
            ApplicationError.PACKER_TEMPLATE_FILE_NOT_FOUND, Constants.PACKER_TEMPLATE_FILE);
      }
      String content = IOUtils.toString(inputStream, Charset.defaultCharset());
      return ApplicationUtil.substituteValues(
          "image", content, this.buildTemplateData(idempotencySha));
    }
  }

  @SneakyThrows
  private Map<String, Object> buildTemplateData(String idempotencySha) {
    String artifactName = this.deployConfig.getArtifactConfig().getName();
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("artifact_name", artifactName);
    dataModel.put("artifact_version", this.deployConfig.getArtifactConfig().getVersion());
    dataModel.put("registry", this.dockerRegistryData.getRegistry());
    dataModel.put("base_image_name", this.deployConfig.getBaseImage().getRepository());
    dataModel.put("base_image_tag", this.deployConfig.getBaseImage().getTag());
    dataModel.put(
        "image_tag", this.deployConfig.getArtifactConfig().getVersion() + "-" + idempotencySha);
    dataModel.put(
        "setup_script",
        this.deployConfig.getArtifactConfig().getHooks().getImageSetup().getScript());
    dataModel.put(
        "setup_script_enabled",
        this.deployConfig.getArtifactConfig().getHooks().getImageSetup().getEnabled());
    dataModel.put(
        "start_script", this.deployConfig.getArtifactConfig().getHooks().getStart().getScript());
    dataModel.put("base_dir", Constants.BASE_DIR);
    Map<String, String> environmentVariables =
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
                this.deployConfig.getExtraEnvVars()));
    dataModel.put("environment_variables", environmentVariables);
    return dataModel;
  }

  public String calculateIdempotencySha(String artifactSha, ImageConfig config) {
    return DigestUtils.sha256Hex(artifactSha + config.getRepository() + config.getTag());
  }
}
