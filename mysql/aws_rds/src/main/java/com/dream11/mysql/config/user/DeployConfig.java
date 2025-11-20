package com.dream11.mysql.config.user;

import com.dream11.mysql.Application;
import com.dream11.mysql.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class DeployConfig implements Config {
  @NotBlank private String username;
  @NotBlank private String password;
  @NotBlank private String version;

  @NotBlank private String engineVersion;

  @Valid @NotNull private WriterConfig writer;

  @Size(min = 1, max = 64)
  @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$")
  private String dbName;

  @Min(1150)
  @Max(65535)
  private Integer port;

  private String storageType;

  @Min(1)
  @Max(35)
  private Integer backupRetentionPeriod;

  @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
  private String preferredBackupWindow;

  @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
  private String preferredMaintenanceWindow;

  private Boolean copyTagsToSnapshot;

  private Boolean deletionProtection;

  private Boolean encryptionAtRest;

  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):kms:[a-z0-9-]+:\\d{12}:key\\/[0-9a-fA-F-]{36}$")
  private String kmsKeyId;

  private Boolean enableIAMDatabaseAuthentication;

  private List<String> enableCloudwatchLogsExports;

  private String snapshotIdentifier;

  private String replicationSourceIdentifier;

  @Pattern(regexp = "^[a-z]{2}-[a-z]+-\\d$")
  private String sourceRegion;

  private String globalClusterIdentifier;

  @Valid private ServerlessV2ScalingConfig serverlessV2ScalingConfiguration;

  @Min(0)
  private Long backtrackWindow;

  @Valid private CredentialsConfig credentials;

  private String clusterParameterGroupName;

  @Valid private ClusterParameterGroupConfig clusterParameterGroupConfig;

  @Valid private DeletionConfig deletionConfig;

  private Map<String, String> tags = new HashMap<>();

  @Valid private List<ReaderConfig> readers;

  @Valid private InstanceConfig instanceConfig;

  @Override
  public void validate() {
    Config.super.validate();
  }

  @SneakyThrows
  public DeployConfig deepCopy() {
    return Application.getObjectMapper()
        .readValue(Application.getObjectMapper().writeValueAsString(this), DeployConfig.class);
  }

  @SneakyThrows
  public DeployConfig mergeWith(String overrides) {
    ObjectMapper objectMapper = Application.getObjectMapper();
    JsonNode node = objectMapper.readValue(objectMapper.writeValueAsString(this), JsonNode.class);
    return objectMapper.readValue(
        objectMapper.readerForUpdating(node).readValue(overrides).toString(), DeployConfig.class);
  }
}
