package com.dream11.mysql.config.user;

import com.dream11.mysql.config.Config;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class UpdateClusterConfig implements Config {
  private String engineVersion;

  @Min(1150)
  @Max(65535)
  private Integer port;

  private String storageType;

  @Min(1)
  @Max(35)
  private Integer backupRetentionPeriod;

  @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
  private String preferredBackupWindow;

  @Pattern(
      regexp =
          "^(mon|tue|wed|thu|fri|sat|sun):\\d{2}:\\d{2}-(mon|tue|wed|thu|fri|sat|sun):\\d{2}:\\d{2}$")
  private String preferredMaintenanceWindow;

  private Boolean copyTagsToSnapshot;

  private Boolean deletionProtection;

  private Boolean enableIAMDatabaseAuthentication;

  @Valid private ServerlessV2ScalingConfig serverlessV2ScalingConfiguration;

  @Min(0)
  private Long backtrackWindow;

  private Map<String, String> tags = new HashMap<>();

  private Boolean applyImmediately;

  @Valid private UpdateInstanceConfig instanceConfig;

  private String clusterParameterGroupName;

  @Valid private ClusterParameterGroupConfig clusterParameterGroupConfig;

  @Valid private CredentialsConfig credentials;

  @Valid private DeletionConfig deletionConfig;
}
