package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.aws.EC2Client;
import com.dream11.application.config.metadata.aws.AwsAccountData;
import com.dream11.application.config.metadata.aws.EC2Data;
import com.dream11.application.config.metadata.aws.NetworkData;
import com.dream11.application.config.user.AMIConfig;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class AMIService {

  @NonNull final EC2Client ec2Client;
  @NonNull final DeployConfig deployConfig;
  @NonNull final EC2Data ec2Data;
  @NonNull final NetworkData networkData;
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

  public void createAMIIfNotExist() {
    String artifactSha =
        Objects.requireNonNull(
            Application.getState().getImage().getArtifactSha(),
            "Invalid state, artifactSha in ami state cannot be null");

    Map<String, AMIConfig> amiConfigToCreate = new HashMap<>();

    this.deployConfig
        .getAmiConfigs()
        .forEach(
            amiConfig -> {
              String idempotencySha =
                  this.calculateIdempotencySha(artifactSha, amiConfig.getFilters());
              String amiName =
                  String.format(
                      "%s-%s-%s-%s*",
                      this.deployConfig.getArtifactConfig().getName(),
                      this.deployConfig.getArtifactConfig().getVersion(),
                      amiConfig.getFilters().get(Constants.ARCHITECTURE_IMAGE_FILTER),
                      idempotencySha);
              Optional<String> ami = this.ec2Client.getAMI(amiName);
              if (ami.isPresent()) {
                log.info(
                    "AMI:[{}] for artifact:[{}@{}] for architecture:[{}] with filters:[{}] and sha:[{}] already exists",
                    ami.get(),
                    this.deployConfig.getArtifactConfig().getName(),
                    this.deployConfig.getArtifactConfig().getVersion(),
                    amiConfig.getFilters().get(Constants.ARCHITECTURE_IMAGE_FILTER),
                    amiConfig.getFilters(),
                    artifactSha);
                Application.getState()
                    .getImage()
                    .addAMI(
                        ami.get(), amiConfig.getFilters().get(Constants.ARCHITECTURE_IMAGE_FILTER));
              } else {
                amiConfigToCreate.putIfAbsent(idempotencySha, amiConfig);
              }
            });

    if (!amiConfigToCreate.isEmpty()) {
      log.info("Generating packer template for AMI creation");
      String substitutedPackerTemplate = this.generatePackerTemplate(amiConfigToCreate);
      this.createPackerFile(substitutedPackerTemplate);
    }
  }

  @SneakyThrows
  private void createPackerFile(String templateContent) {
    FileUtils.writeStringToFile(
        new File(Constants.PACKER_FILE), templateContent, Charset.defaultCharset());
  }

  @SneakyThrows
  private String generatePackerTemplate(Map<String, AMIConfig> amiConfigToCreate) {
    return ApplicationUtil.readTemplateFile(
        Constants.PACKER_TEMPLATE_FILE, this.buildTemplateData(amiConfigToCreate));
  }

  @SneakyThrows
  private Map<String, Object> buildTemplateData(Map<String, AMIConfig> amiConfigToCreate) {
    String artifactName = this.deployConfig.getArtifactConfig().getName();
    return Map.ofEntries(
        Map.entry("artifact_name", artifactName),
        Map.entry("artifact_version", this.deployConfig.getArtifactConfig().getVersion()),
        Map.entry("account_ids", String.join(",", this.ec2Data.getAmi().getSharedAccountIds())),
        Map.entry("region", this.awsAccountData.getRegion()),
        Map.entry("vpc_id", this.networkData.getVpcId()),
        Map.entry("subnet_id", this.networkData.getEc2Subnets().getPrivateSubnets().get(0)),
        Map.entry("unique_id", ApplicationUtil.generateRandomId(Constants.AMI_RANDOM_ID_LENGTH)),
        Map.entry(
            "security_group_ids",
            String.join(",", this.networkData.getEc2SecurityGroups().getInternal())),
        Map.entry("tags", this.tags),
        Map.entry("ami_configs", amiConfigToCreate),
        Map.entry(
            "setup_script",
            this.deployConfig.getArtifactConfig().getHooks().getImageSetup().getScript()),
        Map.entry(
            "setup_script_enabled",
            this.deployConfig.getArtifactConfig().getHooks().getImageSetup().getEnabled()),
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
                    this.ec2Data.getUserData().getEnvironmentVariables(),
                    this.deployConfig.getExtraEnvVars()))));
  }

  /**
   * Sorts the filters and calculate combined SHA of artifact and filters to ensure idempotency.
   *
   * @param artifactSha SHA of the artifact
   * @param filters AMI filters
   * @return SHA of the artifact and filters with length {@link Constants#AMI_HASH_LENGTH}
   */
  private String calculateIdempotencySha(String artifactSha, Map<String, String> filters) {
    return DigestUtils.sha256Hex(artifactSha + new TreeMap<>(filters))
        .substring(0, Constants.AMI_HASH_LENGTH);
  }
}
