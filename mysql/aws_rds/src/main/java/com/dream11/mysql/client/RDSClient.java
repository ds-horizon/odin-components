package com.dream11.mysql.client;

import com.dream11.mysql.config.metadata.aws.RDSData;
import com.dream11.mysql.config.user.ClusterParameterGroupConfig;
import com.dream11.mysql.config.user.DeletionConfig;
import com.dream11.mysql.config.user.DeployConfig;
import com.dream11.mysql.config.user.InstanceConfig;
import com.dream11.mysql.config.user.InstanceParameterGroupConfig;
import com.dream11.mysql.constant.Constants;
import com.dream11.mysql.error.ApplicationError;
import com.dream11.mysql.exception.GenericApplicationException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbClusterParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.CreateDbClusterRequest;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.CreateDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.ModifyDbClusterParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.Parameter;
import software.amazon.awssdk.services.rds.model.RestoreDbClusterFromSnapshotRequest;
import software.amazon.awssdk.services.rds.model.ServerlessV2ScalingConfiguration;
import software.amazon.awssdk.services.rds.model.Tag;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;

@Slf4j
public class RDSClient {
  final RdsClient rdsClient;

  public RDSClient(String region) {
    this.rdsClient =
        RdsClient.builder()
            .region(Region.of(region))
            .overrideConfiguration(
                overrideConfig ->
                    overrideConfig
                        .retryStrategy(RetryMode.STANDARD)
                        .apiCallTimeout(Duration.ofMinutes(2))
                        .apiCallAttemptTimeout(Duration.ofSeconds(30)))
            .build();
  }

  public List<String> createDBCluster(
      String clusterIdentifier,
      String clusterParameterGroupName,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RDSData rdsData) {
    if (deployConfig.getSnapshotIdentifier() != null) {
      return restoreDBClusterFromSnapshot(
          clusterIdentifier,
          deployConfig.getSnapshotIdentifier(),
          tags,
          deployConfig,
          clusterParameterGroupName,
          rdsData);
    } else {
      return createDBClusterFromScratch(
          clusterIdentifier, clusterParameterGroupName, tags, deployConfig, rdsData);
    }
  }

  private List<String> restoreDBClusterFromSnapshot(
      String clusterIdentifier,
      String snapshotIdentifier,
      Map<String, String> tags,
      DeployConfig deployConfig,
      String clusterParameterGroupName,
      RDSData rdsData) {
    log.info(
        "Restoring MySQL cluster {} from snapshot: {}",
        clusterIdentifier,
        deployConfig.getSnapshotIdentifier());
    RestoreDbClusterFromSnapshotRequest.Builder restoreBuilder =
        RestoreDbClusterFromSnapshotRequest.builder()
            .snapshotIdentifier(deployConfig.getSnapshotIdentifier());

    applyCommonConfiguration(
        restoreBuilder::dbClusterIdentifier,
        restoreBuilder::engine,
        restoreBuilder::engineVersion,
        restoreBuilder::dbClusterParameterGroupName,
        restoreBuilder::dbSubnetGroupName,
        restoreBuilder::vpcSecurityGroupIds,
        restoreBuilder::tags,
        restoreBuilder::port,
        restoreBuilder::storageType,
        restoreBuilder::copyTagsToSnapshot,
        restoreBuilder::deletionProtection,
        restoreBuilder::enableIAMDatabaseAuthentication,
        restoreBuilder::kmsKeyId,
        restoreBuilder::enableCloudwatchLogsExports,
        restoreBuilder::serverlessV2ScalingConfiguration,
        restoreBuilder::backtrackWindow,
        clusterIdentifier,
        clusterParameterGroupName,
        tags,
        deployConfig,
        rdsData);

    RestoreDbClusterFromSnapshotRequest request = restoreBuilder.build();

    DBCluster cluster = rdsClient.restoreDBClusterFromSnapshot(request).dbCluster();
    return List.of(cluster.endpoint(), cluster.readerEndpoint());
  }

  private List<String> createDBClusterFromScratch(
      String clusterIdentifier,
      String clusterParameterGroupName,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RDSData rdsData) {
    log.info("Creating MySQL cluster: {}", clusterIdentifier);
    CreateDbClusterRequest.Builder createBuilder = CreateDbClusterRequest.builder();

    applyCommonConfiguration(
        createBuilder::dbClusterIdentifier,
        createBuilder::engine,
        createBuilder::engineVersion,
        createBuilder::dbClusterParameterGroupName,
        createBuilder::dbSubnetGroupName,
        createBuilder::vpcSecurityGroupIds,
        createBuilder::tags,
        createBuilder::port,
        createBuilder::storageType,
        createBuilder::copyTagsToSnapshot,
        createBuilder::deletionProtection,
        createBuilder::enableIAMDatabaseAuthentication,
        createBuilder::kmsKeyId,
        createBuilder::enableCloudwatchLogsExports,
        createBuilder::serverlessV2ScalingConfiguration,
        createBuilder::backtrackWindow,
        clusterIdentifier,
        clusterParameterGroupName,
        tags,
        deployConfig,
        rdsData);

    if (deployConfig.getCredentials() != null) {
      createBuilder.masterUsername(deployConfig.getCredentials().getMasterUsername());
      if (deployConfig.getCredentials().getMasterUserPassword() != null) {
        createBuilder.masterUserPassword(deployConfig.getCredentials().getMasterUserPassword());
      } else if (deployConfig.getCredentials().getMasterUserSecretKmsKeyId() != null) {
        createBuilder.masterUserSecretKmsKeyId(
            deployConfig.getCredentials().getMasterUserSecretKmsKeyId());
      } else if (deployConfig.getCredentials().getManageMasterUserPassword()) {
        createBuilder.manageMasterUserPassword(true);
      } else {
        throw new GenericApplicationException(ApplicationError.INVALID_CREDENTIALS);
      }
    }

    setIfNotNull(createBuilder::databaseName, deployConfig.getDbName());

    setIfNotNull(createBuilder::backupRetentionPeriod, deployConfig.getBackupRetentionPeriod());

    setIfNotNull(createBuilder::preferredBackupWindow, deployConfig.getPreferredBackupWindow());

    setIfNotNull(
        createBuilder::preferredMaintenanceWindow, deployConfig.getPreferredMaintenanceWindow());

    setIfNotNull(createBuilder::storageEncrypted, deployConfig.getEncryptionAtRest());

    setIfNotNull(
        createBuilder::replicationSourceIdentifier, deployConfig.getReplicationSourceIdentifier());

    setIfNotNull(createBuilder::sourceRegion, deployConfig.getSourceRegion());

    setIfNotNull(createBuilder::globalClusterIdentifier, deployConfig.getGlobalClusterIdentifier());

    CreateDbClusterRequest request = createBuilder.build();

    DBCluster cluster = rdsClient.createDBCluster(request).dbCluster();
    return List.of(cluster.endpoint(), cluster.readerEndpoint());
  }

  private void applyCommonConfiguration(
      Consumer<String> clusterIdentifierSetter,
      Consumer<String> engineSetter,
      Consumer<String> engineVersionSetter,
      Consumer<String> clusterParameterGroupNameSetter,
      Consumer<String> subnetGroupNameSetter,
      Consumer<List<String>> securityGroupIdsSetter,
      Consumer<List<Tag>> tagsSetter,
      Consumer<Integer> portSetter,
      Consumer<String> storageTypeSetter,
      Consumer<Boolean> copyTagsSetter,
      Consumer<Boolean> deletionProtectionSetter,
      Consumer<Boolean> iamAuthSetter,
      Consumer<String> kmsKeyIdSetter,
      Consumer<List<String>> logsExportsSetter,
      Consumer<software.amazon.awssdk.services.rds.model.ServerlessV2ScalingConfiguration>
          scalingSetter,
      Consumer<Long> backtrackSetter,
      String clusterIdentifier,
      String clusterParameterGroupName,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RDSData rdsData) {

    clusterIdentifierSetter.accept(clusterIdentifier);
    engineSetter.accept(Constants.ENGINE_TYPE);
    engineVersionSetter.accept(
        deployConfig.getVersion() + ".mysql_aurora." + deployConfig.getEngineVersion());
    clusterParameterGroupNameSetter.accept(clusterParameterGroupName);
    subnetGroupNameSetter.accept(rdsData.getSubnetGroups().get(0));
    securityGroupIdsSetter.accept(rdsData.getSecurityGroups());
    tagsSetter.accept(convertMapToTags(tags));

    setIfNotNull(portSetter, deployConfig.getPort());

    setIfNotNull(storageTypeSetter, deployConfig.getStorageType());

    setIfNotNull(copyTagsSetter, deployConfig.getCopyTagsToSnapshot());

    setIfNotNull(deletionProtectionSetter, deployConfig.getDeletionProtection());

    setIfNotNull(iamAuthSetter, deployConfig.getEnableIAMDatabaseAuthentication());

    setIfNotNull(kmsKeyIdSetter, deployConfig.getKmsKeyId());

    setIfNotNull(logsExportsSetter, deployConfig.getEnableCloudwatchLogsExports());

    setIfNotNull(backtrackSetter, deployConfig.getBacktrackWindow());

    if (deployConfig.getServerlessV2ScalingConfiguration() != null) {
      scalingSetter.accept(
          ServerlessV2ScalingConfiguration.builder()
              .minCapacity(deployConfig.getServerlessV2ScalingConfiguration().getMinCapacity())
              .maxCapacity(deployConfig.getServerlessV2ScalingConfiguration().getMaxCapacity())
              .build());
    }
  }

  public void createDBInstance(
      String instanceIdentifier,
      String clusterIdentifier,
      String instanceParameterGroupName,
      Map<String, String> tags,
      InstanceConfig instanceConfig) {
    log.info("Creating MySQL instance: {}", instanceIdentifier);
    CreateDbInstanceRequest.Builder requestBuilder =
        CreateDbInstanceRequest.builder()
            .dbInstanceIdentifier(instanceIdentifier)
            .dbClusterIdentifier(clusterIdentifier)
            .dbInstanceClass(instanceConfig.getInstanceType())
            .dbParameterGroupName(instanceParameterGroupName)
            .engine(Constants.ENGINE_TYPE)
            .tags(convertMapToTags(tags));

    setIfNotNull(requestBuilder::publiclyAccessible, instanceConfig.getPubliclyAccessible());

    setIfNotNull(
        requestBuilder::autoMinorVersionUpgrade, instanceConfig.getAutoMinorVersionUpgrade());

    setIfNotNull(requestBuilder::deletionProtection, instanceConfig.getDeletionProtection());

    setIfNotNull(
        requestBuilder::enablePerformanceInsights, instanceConfig.getEnablePerformanceInsights());

    setIfNotNull(requestBuilder::availabilityZone, instanceConfig.getAvailabilityZone());

    setIfNotNull(
        requestBuilder::performanceInsightsKMSKeyId,
        instanceConfig.getPerformanceInsightsKmsKeyId());

    setIfNotNull(
        requestBuilder::performanceInsightsRetentionPeriod,
        instanceConfig.getPerformanceInsightsRetentionPeriod());

    if (instanceConfig.getEnhancedMonitoring().getEnabled()) {
      requestBuilder
          .monitoringInterval(instanceConfig.getEnhancedMonitoring().getInterval())
          .monitoringRoleArn(instanceConfig.getEnhancedMonitoring().getMonitoringRoleArn());
    }

    CreateDbInstanceRequest request = requestBuilder.build();
    rdsClient.createDBInstance(request);
  }

  private void performWait(
      String identifier, String type, String waitType, Consumer<RdsWaiter> waitAction) {
    try (RdsWaiter waiter =
        RdsWaiter.builder()
            .client(rdsClient)
            .overrideConfiguration(config -> config.maxAttempts(60))
            .build()) {
      log.info("Waiting for DB {} {} to be {}", type, identifier, waitType);
      waitAction.accept(waiter);
      log.info("DB {} {} is now {}", type, identifier, waitType);
    } catch (Exception e) {
      throw new GenericApplicationException(
          ApplicationError.DB_WAIT_TIMEOUT, type, identifier, waitType);
    }
  }

  public void waitUntilDBClusterAvailable(String clusterIdentifier) {
    performWait(
        clusterIdentifier,
        "cluster",
        "available",
        waiter ->
            waiter.waitUntilDBClusterAvailable(
                builder -> builder.dbClusterIdentifier(clusterIdentifier)));
  }

  public void waitUntilDBInstanceAvailable(String instanceIdentifier) {
    performWait(
        instanceIdentifier,
        "instance",
        "available",
        waiter ->
            waiter.waitUntilDBInstanceAvailable(
                builder -> builder.dbInstanceIdentifier(instanceIdentifier)));
  }

  public void waitUntilDBClusterDeleted(String clusterIdentifier) {
    performWait(
        clusterIdentifier,
        "cluster",
        "deleted",
        waiter ->
            waiter.waitUntilDBClusterDeleted(
                builder -> builder.dbClusterIdentifier(clusterIdentifier)));
  }

  public void waitUntilDBInstanceDeleted(String instanceIdentifier) {
    performWait(
        instanceIdentifier,
        "instance",
        "deleted",
        waiter ->
            waiter.waitUntilDBInstanceDeleted(
                builder -> builder.dbInstanceIdentifier(instanceIdentifier)));
  }

  public void createDBClusterParameterGroup(
      String clusterParameterGroupName, Map<String, String> tags, DeployConfig deployConfig) {
    log.info("Creating cluster parameter group: {}", clusterParameterGroupName);
    CreateDbClusterParameterGroupRequest request =
        CreateDbClusterParameterGroupRequest.builder()
            .dbClusterParameterGroupName(clusterParameterGroupName)
            .dbParameterGroupFamily(Constants.ENGINE_TYPE + deployConfig.getVersion())
            .description(clusterParameterGroupName)
            .tags(convertMapToTags(tags))
            .build();

    rdsClient.createDBClusterParameterGroup(request);

    if (deployConfig.getClusterParameterGroupConfig() != null) {
      configureDBClusterParameters(
          clusterParameterGroupName, deployConfig.getClusterParameterGroupConfig());
    }
  }

  private void configureDBClusterParameters(
      String clusterParameterGroupName, ClusterParameterGroupConfig config) {
    log.info("Configuring cluster parameters for parameter group: {}", clusterParameterGroupName);
    List<Parameter> parameters = new ArrayList<>();

    if (config.getBinlogFormat() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("binlog_format")
              .parameterValue(config.getBinlogFormat())
              .build());
    }

    if (config.getInnodbPrintAllDeadlocks() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("innodb_print_all_deadlocks")
              .parameterValue(config.getInnodbPrintAllDeadlocks())
              .build());
    }

    if (config.getAwsDefaultLambdaRole() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("aws_default_lambda_role")
              .parameterValue(config.getAwsDefaultLambdaRole())
              .build());
    }

    if (config.getAwsDefaultS3Role() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("aws_default_s3_role")
              .parameterValue(config.getAwsDefaultS3Role())
              .build());
    }

    if (config.getAwsDefaultLogsRole() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("aws_default_logs_role")
              .parameterValue(config.getAwsDefaultLogsRole())
              .build());
    }

    if (!parameters.isEmpty()) {
      try {
        ModifyDbClusterParameterGroupRequest request =
            ModifyDbClusterParameterGroupRequest.builder()
                .dbClusterParameterGroupName(clusterParameterGroupName)
                .parameters(parameters)
                .build();

        rdsClient.modifyDBClusterParameterGroup(request);
        log.info(
            "Successfully configured {} parameters for cluster parameter group: {}",
            parameters.size(),
            clusterParameterGroupName);

      } catch (Exception e) {
        log.error("Failed to configure cluster parameters: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to configure cluster parameters", e);
      }
    } else {
      log.info("No cluster parameters to configure");
    }
  }

  public void createDBInstanceParameterGroup(
      String instanceParameterGroupName,
      String version,
      Map<String, String> tags,
      InstanceParameterGroupConfig config) {
    log.info("Creating instance parameter group: {}", instanceParameterGroupName);
    CreateDbParameterGroupRequest request =
        CreateDbParameterGroupRequest.builder()
            .dbParameterGroupName(instanceParameterGroupName)
            .dbParameterGroupFamily(Constants.ENGINE_TYPE + version)
            .description(instanceParameterGroupName)
            .tags(convertMapToTags(tags))
            .build();

    rdsClient.createDBParameterGroup(request);
    if (config != null) {
      configureDBInstanceParameters(instanceParameterGroupName, config);
    }
  }

  private void configureDBInstanceParameters(
      String instanceParameterGroupName, InstanceParameterGroupConfig config) {
    log.info("Configuring instance parameters for parameter group: {}", instanceParameterGroupName);

    List<Parameter> parameters = new ArrayList<>();

    if (config.getInteractiveTimeout() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("interactive_timeout")
              .parameterValue(config.getInteractiveTimeout().toString())
              .build());
    }

    if (config.getWaitTimeout() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("wait_timeout")
              .parameterValue(config.getWaitTimeout().toString())
              .build());
    }

    if (config.getLockWaitTimeout() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("lock_wait_timeout")
              .parameterValue(config.getLockWaitTimeout().toString())
              .build());
    }

    if (config.getLongQueryTime() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("long_query_time")
              .parameterValue(config.getLongQueryTime().toString())
              .build());
    }

    if (config.getMaxAllowedPacket() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("max_allowed_packet")
              .parameterValue(config.getMaxAllowedPacket().toString())
              .build());
    }

    if (config.getSlowQueryLog() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("slow_query_log")
              .parameterValue(config.getSlowQueryLog().toString())
              .build());
    }

    if (config.getTmpTableSize() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("tmp_table_size")
              .parameterValue(config.getTmpTableSize().toString())
              .build());
    }

    if (config.getMaxHeapTableSize() != null) {
      parameters.add(
          Parameter.builder()
              .parameterName("max_heap_table_size")
              .parameterValue(config.getMaxHeapTableSize().toString())
              .build());
    }

    if (!parameters.isEmpty()) {
      try {
        ModifyDbParameterGroupRequest request =
            ModifyDbParameterGroupRequest.builder()
                .dbParameterGroupName(instanceParameterGroupName)
                .parameters(parameters)
                .build();

        rdsClient.modifyDBParameterGroup(request);
        log.info(
            "Successfully configured {} parameters for instance parameter group: {}",
            parameters.size(),
            instanceParameterGroupName);

        for (Parameter param : parameters) {
          log.info("Set parameter: {} = {}", param.parameterName(), param.parameterValue());
        }

      } catch (Exception e) {
        log.error("Failed to configure instance parameters: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to configure instance parameters", e);
      }
    } else {
      log.info("No instance parameters to configure");
    }
  }

  public void describeDBCluster(String clusterIdentifier) {
    this.rdsClient
        .describeDBClusters(request -> request.dbClusterIdentifier(clusterIdentifier))
        .dbClusters()
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new GenericApplicationException(
                    ApplicationError.CLUSTER_NOT_FOUND, clusterIdentifier));
  }

  public void describeDBInstance(String instanceIdentifier) {
    this.rdsClient
        .describeDBInstances(request -> request.dbInstanceIdentifier(instanceIdentifier))
        .dbInstances()
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new GenericApplicationException(
                    ApplicationError.INSTANCE_NOT_FOUND, instanceIdentifier));
  }

  public void describeDBClusterParameterGroup(String clusterParameterGroupName) {
    this.rdsClient
        .describeDBClusterParameterGroups(
            request -> request.dbClusterParameterGroupName(clusterParameterGroupName))
        .dbClusterParameterGroups()
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new GenericApplicationException(
                    ApplicationError.CLUSTER_PARAMETER_GROUP_NOT_FOUND, clusterParameterGroupName));
  }

  public void describeDBParameterGroup(String instanceParameterGroupName) {
    this.rdsClient
        .describeDBParameterGroups(
            request -> request.dbParameterGroupName(instanceParameterGroupName))
        .dbParameterGroups()
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new GenericApplicationException(
                    ApplicationError.INSTANCE_PARAMETER_GROUP_NOT_FOUND,
                    instanceParameterGroupName));
  }

  public void deleteDBCluster(String clusterIdentifier, DeletionConfig deletionConfig) {
    this.rdsClient.deleteDBCluster(
        request -> {
          request.dbClusterIdentifier(clusterIdentifier);
          if (!deletionConfig.getSkipFinalSnapshot()) {
            request.skipFinalSnapshot(false);
            request.finalDBSnapshotIdentifier(deletionConfig.getFinalSnapshotIdentifier());
          } else {
            request.skipFinalSnapshot(true);
          }
        });
  }

  public void deleteDBInstance(String instanceIdentifier, DeletionConfig deletionConfig) {
    this.rdsClient.deleteDBInstance(
        request -> {
          request.dbInstanceIdentifier(instanceIdentifier);
          if (!deletionConfig.getSkipFinalSnapshot()) {
            request.skipFinalSnapshot(false);
            request.finalDBSnapshotIdentifier(deletionConfig.getFinalSnapshotIdentifier());
          } else {
            request.skipFinalSnapshot(true);
          }
        });
  }

  public void deleteDBClusterParameterGroup(String clusterParameterGroupName) {
    this.rdsClient.deleteDBClusterParameterGroup(
        request -> request.dbClusterParameterGroupName(clusterParameterGroupName));
  }

  public void deleteDBParameterGroup(String instanceParameterGroupName) {
    this.rdsClient.deleteDBParameterGroup(
        request -> request.dbParameterGroupName(instanceParameterGroupName));
  }

  private List<Tag> convertMapToTags(Map<String, String> tagMap) {
    if (tagMap == null || tagMap.isEmpty()) {
      return new ArrayList<>();
    }

    List<Tag> tags = new ArrayList<>();
    for (Map.Entry<String, String> entry : tagMap.entrySet()) {
      tags.add(Tag.builder().key(entry.getKey()).value(entry.getValue()).build());
    }
    return tags;
  }

  private <T> void setIfNotNull(Consumer<T> setter, T value) {
    if (value != null) {
      setter.accept(value);
    }
  }
}
