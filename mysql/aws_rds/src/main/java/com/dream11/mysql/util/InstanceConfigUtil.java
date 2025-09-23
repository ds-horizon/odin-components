package com.dream11.mysql.util;

import com.dream11.mysql.config.user.InstanceConfig;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceRequest;

/** Utility class for common instance configuration operations */
public class InstanceConfigUtil {

  /**
   * Applies common instance configuration to a CreateDbInstanceRequest builder This method can be
   * used for both Writer and Reader instances since they both extend InstanceConfig
   *
   * @param requestBuilder The request builder to configure
   * @param instanceConfig The instance configuration (WriterConfig or ReaderConfig)
   * @param engineVersion The engine version to use (optional)
   */
  public static void applyCommonInstanceConfig(
      CreateDbInstanceRequest.Builder requestBuilder,
      InstanceConfig instanceConfig,
      String engineVersion) {

    // Required fields
    requestBuilder.dbInstanceClass(instanceConfig.getInstanceType()).engine("aurora-mysql");

    // Optional engine version
    if (engineVersion != null) {
      requestBuilder.engineVersion(engineVersion);
    }

    // Optional common fields - only set if provided
    if (instanceConfig.getPubliclyAccessible() != null) {
      requestBuilder.publiclyAccessible(instanceConfig.getPubliclyAccessible());
    }

    if (instanceConfig.getAutoMinorVersionUpgrade() != null) {
      requestBuilder.autoMinorVersionUpgrade(instanceConfig.getAutoMinorVersionUpgrade());
    }

    if (instanceConfig.getDeletionProtection() != null) {
      requestBuilder.deletionProtection(instanceConfig.getDeletionProtection());
    }

    if (instanceConfig.getEnablePerformanceInsights() != null) {
      requestBuilder.enablePerformanceInsights(instanceConfig.getEnablePerformanceInsights());
    }

    if (instanceConfig.getAvailabilityZone() != null) {
      requestBuilder.availabilityZone(instanceConfig.getAvailabilityZone());
    }

    if (instanceConfig.getInstanceParameterGroupName() != null) {
      requestBuilder.dbParameterGroupName(instanceConfig.getInstanceParameterGroupName());
    }

    if (instanceConfig.getPerformanceInsightsKmsKeyId() != null) {
      requestBuilder.performanceInsightsKMSKeyId(instanceConfig.getPerformanceInsightsKmsKeyId());
    }

    if (instanceConfig.getPerformanceInsightsRetentionPeriod() != null) {
      requestBuilder.performanceInsightsRetentionPeriod(
          instanceConfig.getPerformanceInsightsRetentionPeriod());
    }

    if (instanceConfig.getEnhancedMonitoring() != null
        && instanceConfig.getEnhancedMonitoring().getEnabled()) {
      requestBuilder
          .monitoringInterval(instanceConfig.getEnhancedMonitoring().getInterval())
          .monitoringRoleArn(instanceConfig.getEnhancedMonitoring().getMonitoringRoleArn());
    }

    if (instanceConfig.getNetworkType() != null) {
      requestBuilder.networkType(instanceConfig.getNetworkType());
    }
  }
}
