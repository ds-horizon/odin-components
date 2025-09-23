package com.dream11.mysql.config.user;

import com.dream11.mysql.Application;
import com.dream11.mysql.config.Config;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
  @JsonProperty("version")
  @NotNull
  private String version;

  @JsonProperty("engineVersion")
  @NotNull
  private String engineVersion;

  @JsonProperty("writer")
  @Valid
  @NotNull
  private WriterConfig writer;

  @JsonProperty("dbName")
  @Size(min = 1, max = 64)
  @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$")
  private String dbName;

  @JsonProperty("port")
  @Min(1150)
  @Max(65535)
  private Integer port;

  @JsonProperty("storageType")
  private String storageType;

  @JsonProperty("backupRetentionPeriod")
  @Min(1)
  @Max(35)
  private Integer backupRetentionPeriod;

  @JsonProperty("preferredBackupWindow")
  @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
  private String preferredBackupWindow;

  @JsonProperty("preferredMaintenanceWindow")
  @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
  private String preferredMaintenanceWindow;

  @JsonProperty("copyTagsToSnapshot")
  private Boolean copyTagsToSnapshot;

  @JsonProperty("deletionProtection")
  private Boolean deletionProtection;

  @JsonProperty("encryptionAtRest")
  private Boolean encryptionAtRest;

  @JsonProperty("kmsKeyId")
  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):kms:[a-z0-9-]+:\\d{12}:key\\/[0-9a-fA-F-]{36}$")
  private String kmsKeyId;

  @JsonProperty("enableIAMDatabaseAuthentication")
  private Boolean enableIAMDatabaseAuthentication;

  @JsonProperty("enableCloudwatchLogsExports")
  private List<String> enableCloudwatchLogsExports;

  @JsonProperty("snapshotIdentifier")
  private String snapshotIdentifier;

  @JsonProperty("replicationSourceIdentifier")
  private String replicationSourceIdentifier;

  @JsonProperty("sourceRegion")
  @Pattern(regexp = "^[a-z]{2}-[a-z]+-\\d$")
  private String sourceRegion;

  @JsonProperty("globalClusterIdentifier")
  private String globalClusterIdentifier;

  @JsonProperty("serverlessV2ScalingConfiguration")
  @Valid
  private ServerlessV2ScalingConfig serverlessV2ScalingConfiguration;

  @JsonProperty("backtrackWindow")
  @Min(0)
  private Long backtrackWindow;

  @JsonProperty("credentials")
  @Valid
  private CredentialsConfig credentials;

  @JsonProperty("clusterParameterGroupName")
  private String clusterParameterGroupName;

  @JsonProperty("clusterParameterGroupConfig")
  @Valid
  private ClusterParameterGroupConfig clusterParameterGroupConfig;

  @JsonProperty("deletion")
  @Valid
  private DeletionConfig deletion;

  @JsonProperty("tags")
  private Map<String, String> tags = new HashMap<>();

  @JsonProperty("readers")
  @Valid
  private List<ReaderConfig> readers;

  @Override
  public void validate() {
    Config.super.validate();
    // Add MySQL RDS specific validations if needed
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
