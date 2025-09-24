package com.dream11.mysql.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public abstract class InstanceConfig {

  @NotNull
  @Pattern(
      regexp =
          "^(db\\.(?:[trm][0-9][a-z]?\\.(?:micro|small|medium|large|xlarge|\\d+x?large))|db\\.serverless)$")
  private String instanceType;

  @Pattern(regexp = "^[a-z]{2}-[a-z]+-\\d[a-z]?$")
  private String availabilityZone;

  @Min(0)
  @Max(15)
  private Integer promotionTier;

  private Boolean publiclyAccessible;

  private Boolean autoMinorVersionUpgrade;

  private Boolean deletionProtection;

  private Boolean enablePerformanceInsights;

  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):kms:[a-z0-9-]+:\\d{12}:key\\/[0-9a-fA-F-]{36}$")
  private String performanceInsightsKmsKeyId;

  private Integer performanceInsightsRetentionPeriod;

  @Valid private EnhancedMonitoringConfig enhancedMonitoring;

  private String instanceParameterGroupName;

  @Valid private InstanceParameterGroupConfig instanceParameterGroupConfig;

  private String networkType;
}
