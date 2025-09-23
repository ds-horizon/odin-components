package com.dream11.mysql.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Abstract base class for database instance configuration (both Writer and Reader instances)
 * Contains all common fields that are shared between Writer and Reader instances
 */
@Data
public abstract class InstanceConfig {

  @JsonProperty("instanceType")
  @NotNull
  @Pattern(
      regexp =
          "^(db\\.(?:[trm][0-9][a-z]?\\.(?:micro|small|medium|large|xlarge|\\d+x?large))|db\\.serverless)$")
  private String instanceType;

  @JsonProperty("availabilityZone")
  @Pattern(regexp = "^[a-z]{2}-[a-z]+-\\d[a-z]?$")
  private String availabilityZone;

  @JsonProperty("promotionTier")
  @Min(0)
  @Max(15)
  private Integer promotionTier;

  @JsonProperty("publiclyAccessible")
  private Boolean publiclyAccessible;

  @JsonProperty("autoMinorVersionUpgrade")
  private Boolean autoMinorVersionUpgrade;

  @JsonProperty("deletionProtection")
  private Boolean deletionProtection;

  @JsonProperty("enablePerformanceInsights")
  private Boolean enablePerformanceInsights;

  @JsonProperty("performanceInsightsKmsKeyId")
  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):kms:[a-z0-9-]+:\\d{12}:key\\/[0-9a-fA-F-]{36}$")
  private String performanceInsightsKmsKeyId;

  @JsonProperty("performanceInsightsRetentionPeriod")
  private Integer performanceInsightsRetentionPeriod;

  @JsonProperty("enhancedMonitoring")
  @Valid
  private EnhancedMonitoringConfig enhancedMonitoring;

  @JsonProperty("instanceParameterGroupName")
  private String instanceParameterGroupName;

  @JsonProperty("instanceParameterGroupConfig")
  @Valid
  private InstanceParameterGroupConfig instanceParameterGroupConfig;

  @JsonProperty("networkType")
  private String networkType;
}
