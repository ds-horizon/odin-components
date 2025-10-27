package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.aws.LaunchTemplateClient;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.config.metadata.aws.AwsAccountData;
import com.dream11.application.config.metadata.aws.EC2Data;
import com.dream11.application.config.metadata.aws.NetworkData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DeploymentStrategy;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.state.AMIState;
import com.dream11.application.state.State;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class LaunchTemplateService {

  @NonNull final DeployConfig deployConfig;

  @NonNull final LaunchTemplateClient launchTemplateClient;

  @NonNull final EC2Data ec2Data;
  @NonNull final NetworkData networkData;
  @NonNull ComponentMetadata componentMetadata;
  @NonNull final AwsAccountData awsAccountData;

  Map<String, String> tags;

  @Inject
  private void init() {
    this.tags =
        ApplicationUtil.merge(
            List.of(
                this.awsAccountData.getTags(),
                this.ec2Data.getTags(),
                this.deployConfig.getTags(),
                Constants.COMPONENT_TAGS));
  }

  public Map<LaunchTemplate, String> createLaunchTemplates(
      String name, Map<String, String> envVars) {
    return Application.getState().getImage().getAmis().stream()
        .collect(
            Collectors.toMap(
                ami ->
                    createLaunchTemplate(
                        name,
                        envVars,
                        ami.getId(),
                        this.deployConfig.getAutoScalingGroupConfig().getInstances().stream()
                            .filter(
                                instance ->
                                    instance.getArchitecture().equals(ami.getArchitecture()))
                            .findFirst()
                            .orElseThrow(
                                () ->
                                    new GenericApplicationException(
                                        ApplicationError.NO_INSTANCE_FOUND_FOR_ARCHITECTURE,
                                        ami.getArchitecture()))
                            .getTypes()
                            .get(0),
                        ami.getArchitecture()),
                AMIState.AMI::getArchitecture));
  }

  public LaunchTemplate createLaunchTemplate(
      String name,
      Map<String, String> envVars,
      String amiId,
      String instanceType,
      String architecture) {
    String ltNameWithArchitecture = name + "-" + architecture;
    LaunchTemplate launchTemplate =
        this.launchTemplateClient.create(
            ltNameWithArchitecture,
            amiId,
            instanceType,
            this.ec2Data.getEc2KeyName(),
            this.ec2Data.getIamInstanceProfile(),
            this.networkData.getEc2SecurityGroups().getInternal(),
            this.generateUserdata(envVars),
            this.deployConfig.getEbsConfig(),
            this.deployConfig.getAutoScalingGroupConfig().getImdsv2(),
            ApplicationUtil.merge(
                List.of(
                    this.tags,
                    Map.of(
                        Constants.NAME_TAG,
                        name,
                        Constants.ARTIFACT_NAME_TAG,
                        this.deployConfig.getArtifactConfig().getName(),
                        Constants.ARTIFACT_VERSION_TAG,
                        this.deployConfig.getArtifactConfig().getVersion(),
                        Constants.DEPLOYMENT_STRATEGY_TAG,
                        DeploymentStrategy.BLUE_GREEN.name()))));
    log.info(
        "Launch Template:[{}] for architecture:[{}] created successfully",
        ltNameWithArchitecture,
        architecture);
    Application.getState().addLtState(launchTemplate.launchTemplateId(), architecture);
    return launchTemplate;
  }

  @SneakyThrows
  private String generateUserdata(Map<String, String> envVars) {
    try (InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(Constants.USERDATA_TEMPLATE_FILE)) {
      if (Objects.isNull(inputStream)) {
        throw new GenericApplicationException(
            ApplicationError.TEMPLATE_FILE_NOT_FOUND, Constants.USERDATA_TEMPLATE_FILE);
      }
      String content = IOUtils.toString(inputStream, Charset.defaultCharset());
      return Base64.getEncoder()
          .encodeToString(
              ApplicationUtil.substituteValues("userdata", content, this.buildTemplateData(envVars))
                  .getBytes());
    }
  }

  private Map<String, Object> buildTemplateData(Map<String, String> envVars) {
    String appDirectory =
        Constants.APPLICATION_DIRECTORY.apply(this.deployConfig.getArtifactConfig().getName());
    Map<String, String> environmentVariables =
        ApplicationUtil.merge(
            List.of(
                this.ec2Data.getUserData().getEnvironmentVariables(),
                this.deployConfig.getExtraEnvVars(),
                Map.of(
                    "COMPONENT_NAME",
                    this.componentMetadata.getComponentName(),
                    "ARTIFACT_NAME",
                    this.deployConfig.getArtifactConfig().getName(),
                    "ARTIFACT_VERSION",
                    this.deployConfig.getArtifactConfig().getVersion(),
                    "APP_DIR",
                    appDirectory,
                    "START_SCRIPT_PATH",
                    this.deployConfig.getArtifactConfig().getHooks().getStart().getScript(),
                    "STOP_SCRIPT_PATH",
                    this.deployConfig.getArtifactConfig().getHooks().getStop().getScript(),
                    "DEPLOYMENT_TYPE",
                    Constants.DEPLOYMENT_TYPE),
                envVars));
    Map<String, Object> tagData =
        Map.of(
            "deployment_strategy_tag",
            Constants.DEPLOYMENT_STRATEGY_TAG,
            "artifact_name_tag",
            Constants.ARTIFACT_NAME_TAG,
            "artifact_version_tag",
            Constants.ARTIFACT_VERSION_TAG);

    return ApplicationUtil.merge(
        List.of(
            Map.of(
                "environment_variables",
                environmentVariables,
                "pre_start_userdata",
                new String(Base64.getDecoder().decode(this.ec2Data.getUserData().getPreStart())),
                "post_start_userdata",
                new String(Base64.getDecoder().decode(this.ec2Data.getUserData().getPostStart())),
                "start_script_path",
                this.deployConfig.getArtifactConfig().getHooks().getStart().getScript(),
                "stop_script_path",
                this.deployConfig.getArtifactConfig().getHooks().getStop().getScript(),
                "application_directory",
                appDirectory,
                "component_name",
                this.componentMetadata.getComponentName()),
            tagData));
  }

  public void deleteLaunchTemplates() {
    log.info("Deleting launch templates");
    State state = Application.getState();
    List.copyOf(state.getLt())
        .forEach(launchTemplateState -> this.deleteLaunchTemplate(launchTemplateState.getId()));
  }

  public void deleteLaunchTemplates(List<String> ids) {
    ids.forEach(this::deleteLaunchTemplate);
  }

  public void deleteLaunchTemplate(String id) {
    State state = Application.getState();
    log.debug("Deleting launch template:[{}]", id);
    this.launchTemplateClient.delete(id);
    log.info("Deleted launch template:[{}]", id);
    state.removeLtState(id);
  }

  public LaunchTemplate describeLaunchTemplate(String id) {
    return this.launchTemplateClient.describe(id);
  }
}
