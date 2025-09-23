package com.dream11.mysql.client;

import com.dream11.mysql.config.metadata.aws.RDSData;
import com.dream11.mysql.config.user.ClusterParameterGroupConfig;
import com.dream11.mysql.config.user.DeployConfig;
import com.dream11.mysql.config.user.InstanceConfig;
import com.dream11.mysql.config.user.InstanceParameterGroupConfig;
import com.dream11.mysql.error.ApplicationError;
import com.dream11.mysql.exception.GenericApplicationException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
            .credentialsProvider(DefaultCredentialsProvider.create())
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
    boolean isRestoreFromSnapshot = deployConfig.getSnapshotIdentifier() != null;

    if (isRestoreFromSnapshot) {
      log.info(
          "Restoring MySQL cluster {} from snapshot: {}",
          clusterIdentifier,
          deployConfig.getSnapshotIdentifier());
      RestoreDbClusterFromSnapshotRequest.Builder restoreBuilder =
          RestoreDbClusterFromSnapshotRequest.builder()
              .snapshotIdentifier(deployConfig.getSnapshotIdentifier());

      applyCommonConfiguration(
          clusterId -> restoreBuilder.dbClusterIdentifier(clusterId),
          engine -> restoreBuilder.engine(engine),
          engineVersion -> restoreBuilder.engineVersion(engineVersion),
          clusterParamGroupName ->
              restoreBuilder.dbClusterParameterGroupName(clusterParamGroupName),
          subnetGroupName -> restoreBuilder.dbSubnetGroupName(subnetGroupName),
          securityGroupIds -> restoreBuilder.vpcSecurityGroupIds(securityGroupIds),
          tagList -> restoreBuilder.tags(tagList),
          port -> restoreBuilder.port(port),
          storageType -> restoreBuilder.storageType(storageType),
          copyTags -> restoreBuilder.copyTagsToSnapshot(copyTags),
          deletionProtection -> restoreBuilder.deletionProtection(deletionProtection),
          iamAuth -> restoreBuilder.enableIAMDatabaseAuthentication(iamAuth),
          kmsKeyId -> restoreBuilder.kmsKeyId(kmsKeyId),
          logsExports -> restoreBuilder.enableCloudwatchLogsExports(logsExports),
          scaling -> restoreBuilder.serverlessV2ScalingConfiguration(scaling),
          backtrack -> restoreBuilder.backtrackWindow(backtrack),
          clusterIdentifier,
          clusterParameterGroupName,
          tags,
          deployConfig,
          rdsData);

      RestoreDbClusterFromSnapshotRequest request = restoreBuilder.build();

      DBCluster cluster = rdsClient.restoreDBClusterFromSnapshot(request).dbCluster();
      return List.of(cluster.endpoint(), cluster.readerEndpoint());
    } else {
      log.info("Creating MySQL cluster: {}", clusterIdentifier);
      CreateDbClusterRequest.Builder createBuilder = CreateDbClusterRequest.builder();

      applyCommonConfiguration(
          clusterId -> createBuilder.dbClusterIdentifier(clusterId),
          engine -> createBuilder.engine(engine),
          engineVersion -> createBuilder.engineVersion(engineVersion),
          clusterParamGroupName -> createBuilder.dbClusterParameterGroupName(clusterParamGroupName),
          subnetGroupName -> createBuilder.dbSubnetGroupName(subnetGroupName),
          securityGroupIds -> createBuilder.vpcSecurityGroupIds(securityGroupIds),
          tagList -> createBuilder.tags(tagList),
          port -> createBuilder.port(port),
          storageType -> createBuilder.storageType(storageType),
          copyTags -> createBuilder.copyTagsToSnapshot(copyTags),
          deletionProtection -> createBuilder.deletionProtection(deletionProtection),
          iamAuth -> createBuilder.enableIAMDatabaseAuthentication(iamAuth),
          kmsKeyId -> createBuilder.kmsKeyId(kmsKeyId),
          logsExports -> createBuilder.enableCloudwatchLogsExports(logsExports),
          scaling -> createBuilder.serverlessV2ScalingConfiguration(scaling),
          backtrack -> createBuilder.backtrackWindow(backtrack),
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
        } else if (deployConfig.getCredentials().getManageMasterUserPassword() == true) {
          createBuilder.manageMasterUserPassword(
              deployConfig.getCredentials().getManageMasterUserPassword());
        } else {
          throw new GenericApplicationException(ApplicationError.INVALID_CREDENTIALS);
        }
      }

      if (deployConfig.getDbName() != null) {
        createBuilder.databaseName(deployConfig.getDbName());
      }

      if (deployConfig.getBackupRetentionPeriod() != null) {
        createBuilder.backupRetentionPeriod(deployConfig.getBackupRetentionPeriod());
      }

      if (deployConfig.getPreferredBackupWindow() != null) {
        createBuilder.preferredBackupWindow(deployConfig.getPreferredBackupWindow());
      }

      if (deployConfig.getPreferredMaintenanceWindow() != null) {
        createBuilder.preferredMaintenanceWindow(deployConfig.getPreferredMaintenanceWindow());
      }

      if (deployConfig.getEncryptionAtRest() != null) {
        createBuilder.storageEncrypted(deployConfig.getEncryptionAtRest());
      }

      if (deployConfig.getReplicationSourceIdentifier() != null) {
        createBuilder.replicationSourceIdentifier(deployConfig.getReplicationSourceIdentifier());

        if (deployConfig.getSourceRegion() != null) {
          createBuilder.sourceRegion(deployConfig.getSourceRegion());
        }
      }

      if (deployConfig.getGlobalClusterIdentifier() != null) {
        createBuilder.globalClusterIdentifier(deployConfig.getGlobalClusterIdentifier());
      }

      CreateDbClusterRequest request = createBuilder.build();

      DBCluster cluster = rdsClient.createDBCluster(request).dbCluster();
      return List.of(cluster.endpoint(), cluster.readerEndpoint());
    }
  }

  private void applyCommonConfiguration(
      java.util.function.Consumer<String> clusterIdentifierSetter,
      java.util.function.Consumer<String> engineSetter,
      java.util.function.Consumer<String> engineVersionSetter,
      java.util.function.Consumer<String> clusterParameterGroupNameSetter,
      java.util.function.Consumer<String> subnetGroupNameSetter,
      java.util.function.Consumer<java.util.List<String>> securityGroupIdsSetter,
      java.util.function.Consumer<java.util.List<Tag>> tagsSetter,
      java.util.function.Consumer<Integer> portSetter,
      java.util.function.Consumer<String> storageTypeSetter,
      java.util.function.Consumer<Boolean> copyTagsSetter,
      java.util.function.Consumer<Boolean> deletionProtectionSetter,
      java.util.function.Consumer<Boolean> iamAuthSetter,
      java.util.function.Consumer<String> kmsKeyIdSetter,
      java.util.function.Consumer<java.util.List<String>> logsExportsSetter,
      java.util.function.Consumer<
              software.amazon.awssdk.services.rds.model.ServerlessV2ScalingConfiguration>
          scalingSetter,
      java.util.function.Consumer<Long> backtrackSetter,
      String clusterIdentifier,
      String clusterParameterGroupName,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RDSData rdsData) {

    clusterIdentifierSetter.accept(clusterIdentifier);
    engineSetter.accept("aurora-mysql");
    engineVersionSetter.accept(
        deployConfig.getVersion() + ".mysql_aurora." + deployConfig.getEngineVersion());
    clusterParameterGroupNameSetter.accept(clusterParameterGroupName);
    subnetGroupNameSetter.accept(rdsData.getSubnetGroups().get(0));
    securityGroupIdsSetter.accept(rdsData.getSecurityGroups());
    tagsSetter.accept(convertMapToTags(tags));

    if (deployConfig.getPort() != null) {
      portSetter.accept(deployConfig.getPort());
    }

    if (deployConfig.getStorageType() != null) {
      storageTypeSetter.accept(deployConfig.getStorageType());
    }

    if (deployConfig.getCopyTagsToSnapshot() != null) {
      copyTagsSetter.accept(deployConfig.getCopyTagsToSnapshot());
    }

    if (deployConfig.getDeletionProtection() != null) {
      deletionProtectionSetter.accept(deployConfig.getDeletionProtection());
    }

    if (deployConfig.getEnableIAMDatabaseAuthentication() != null) {
      iamAuthSetter.accept(deployConfig.getEnableIAMDatabaseAuthentication());
    }

    if (deployConfig.getKmsKeyId() != null) {
      kmsKeyIdSetter.accept(deployConfig.getKmsKeyId());
    }

    if (deployConfig.getEnableCloudwatchLogsExports() != null) {
      logsExportsSetter.accept(deployConfig.getEnableCloudwatchLogsExports());
    }

    if (deployConfig.getServerlessV2ScalingConfiguration() != null) {
      scalingSetter.accept(
          ServerlessV2ScalingConfiguration.builder()
              .minCapacity(deployConfig.getServerlessV2ScalingConfiguration().getMinCapacity())
              .maxCapacity(deployConfig.getServerlessV2ScalingConfiguration().getMaxCapacity())
              .build());
    }

    if (deployConfig.getBacktrackWindow() != null) {
      backtrackSetter.accept(deployConfig.getBacktrackWindow());
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
            .engine("aurora-mysql")
            .tags(convertMapToTags(tags));

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

    CreateDbInstanceRequest request = requestBuilder.build();
    rdsClient.createDBInstance(request);
  }

  public void waitUntilDBClusterAvailable(String clusterIdentifier) {
    try (RdsWaiter waiter =
        RdsWaiter.builder()
            .client(rdsClient)
            .overrideConfiguration(config -> config.maxAttempts(60))
            .build()) {
      log.info("Waiting for DB cluster {} to become available...", clusterIdentifier);
      waiter.waitUntilDBClusterAvailable(builder -> builder.dbClusterIdentifier(clusterIdentifier));
    } catch (Exception e) {
      throw new GenericApplicationException(
          ApplicationError.CLUSTER_AVAILABLE_TIMEOUT, clusterIdentifier);
    }
  }

  public void waitUntilDBInstanceAvailable(String instanceIdentifier) {
    try (RdsWaiter waiter =
        RdsWaiter.builder()
            .client(rdsClient)
            .overrideConfiguration(config -> config.maxAttempts(60))
            .build()) {
      log.info("Waiting for DB instance {} to become available...", instanceIdentifier);
      waiter.waitUntilDBInstanceAvailable(
          builder -> builder.dbInstanceIdentifier(instanceIdentifier));
    } catch (Exception e) {
      throw new GenericApplicationException(
          ApplicationError.INSTANCE_AVAILABLE_TIMEOUT, instanceIdentifier);
    }
  }

  public void waitUntilDBClusterDeleted(String clusterIdentifier) {
    try (RdsWaiter waiter =
        RdsWaiter.builder()
            .client(rdsClient)
            .overrideConfiguration(config -> config.maxAttempts(60))
            .build()) {
      log.info("Waiting for DB cluster {} to be deleted...", clusterIdentifier);
      waiter.waitUntilDBClusterDeleted(builder -> builder.dbClusterIdentifier(clusterIdentifier));
    } catch (Exception e) {
      throw new GenericApplicationException(
          ApplicationError.CLUSTER_DELETE_TIMEOUT, clusterIdentifier);
    }
  }

  public void waitUntilDBInstanceDeleted(String instanceIdentifier) {
    try (RdsWaiter waiter =
        RdsWaiter.builder()
            .client(rdsClient)
            .overrideConfiguration(config -> config.maxAttempts(60))
            .build()) {
      log.info("Waiting for DB instance {} to be deleted...", instanceIdentifier);
      waiter.waitUntilDBInstanceDeleted(
          builder -> builder.dbInstanceIdentifier(instanceIdentifier));
    } catch (Exception e) {
      throw new GenericApplicationException(
          ApplicationError.INSTANCE_DELETE_TIMEOUT, instanceIdentifier);
    }
  }

  public void createClusterParameterGroup(
      String clusterParameterGroupName, Map<String, String> tags, DeployConfig deployConfig) {
    log.info("Creating cluster parameter group: {}", clusterParameterGroupName);
    CreateDbClusterParameterGroupRequest request =
        CreateDbClusterParameterGroupRequest.builder()
            .dbClusterParameterGroupName(clusterParameterGroupName)
            .dbParameterGroupFamily("aurora-mysql" + deployConfig.getVersion())
            .description(clusterParameterGroupName)
            .tags(convertMapToTags(tags))
            .build();

    rdsClient.createDBClusterParameterGroup(request);

    if (deployConfig.getClusterParameterGroupConfig() != null) {
      configureClusterParameters(
          clusterParameterGroupName, deployConfig.getClusterParameterGroupConfig());
    }
  }

  private void configureClusterParameters(
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

  public void createInstanceParameterGroup(
      String instanceParameterGroupName,
      String version,
      Map<String, String> tags,
      InstanceParameterGroupConfig config) {
    log.info("Creating instance parameter group: {}", instanceParameterGroupName);
    CreateDbParameterGroupRequest request =
        CreateDbParameterGroupRequest.builder()
            .dbParameterGroupName(instanceParameterGroupName)
            .dbParameterGroupFamily("aurora-mysql" + version)
            .description(instanceParameterGroupName)
            .tags(convertMapToTags(tags))
            .build();

    rdsClient.createDBParameterGroup(request);
    if (config != null) {
      configureInstanceParameters(instanceParameterGroupName, config);
    }
  }

  private void configureInstanceParameters(
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

  public void deleteDBCluster(String clusterIdentifier) {
    this.rdsClient.deleteDBCluster(
        request -> request.dbClusterIdentifier(clusterIdentifier).skipFinalSnapshot(true));
  }

  public void deleteDBInstance(String instanceIdentifier) {
    this.rdsClient.deleteDBInstance(
        request -> request.dbInstanceIdentifier(instanceIdentifier).skipFinalSnapshot(true));
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
}
