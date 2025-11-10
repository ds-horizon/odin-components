package com.dream11.mysql.service;

import com.dream11.mysql.Application;
import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.config.metadata.ComponentMetadata;
import com.dream11.mysql.config.metadata.aws.AwsAccountData;
import com.dream11.mysql.config.metadata.aws.RDSData;
import com.dream11.mysql.config.user.AddReadersConfig;
import com.dream11.mysql.config.user.ClusterParameterGroupConfig;
import com.dream11.mysql.config.user.DeployConfig;
import com.dream11.mysql.config.user.FailoverConfig;
import com.dream11.mysql.config.user.InstanceConfig;
import com.dream11.mysql.config.user.InstanceParameterGroupConfig;
import com.dream11.mysql.config.user.ReaderConfig;
import com.dream11.mysql.config.user.RebootConfig;
import com.dream11.mysql.config.user.RemoveReadersConfig;
import com.dream11.mysql.config.user.UpdateClusterConfig;
import com.dream11.mysql.constant.Constants;
import com.dream11.mysql.error.ApplicationError;
import com.dream11.mysql.exception.GenericApplicationException;
import com.dream11.mysql.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RDSService {
  @NonNull final DeployConfig deployConfig;
  @NonNull final ComponentMetadata componentMetadata;
  @NonNull final RDSClient rdsClient;
  @NonNull final AwsAccountData awsAccountData;
  @NonNull final RDSData rdsData;
  @NonNull final AddReadersConfig addReadersConfig;
  @NonNull final RemoveReadersConfig removeReadersConfig;
  @NonNull final FailoverConfig failoverConfig;
  @NonNull final RebootConfig rebootConfig;
  @NonNull final UpdateClusterConfig updateClusterConfig;

  public void deploy() {
    List<Callable<Void>> tasks = new ArrayList<>();
    String identifier = Application.getState().getIdentifier();
    if (identifier == null) {
      identifier = ApplicationUtil.generateRandomId(Constants.RANDOM_ID_LENGTH);
      Application.getState().setIdentifier(identifier);
    }
    String name =
        ApplicationUtil.joinByHyphen(
            this.componentMetadata.getComponentName(), this.componentMetadata.getEnvName());

    Map<String, String> tags =
        ApplicationUtil.merge(
            List.of(
                this.deployConfig.getTags(),
                this.awsAccountData.getTags(),
                Constants.COMPONENT_TAGS));

    List<String> parameterGroups = this.createParameterGroups(name, identifier, tags);
    String clusterParameterGroupName = parameterGroups.get(0);
    String instanceParameterGroupName = parameterGroups.get(1);

    String clusterIdentifier =
        this.createClusterAndWait(name, identifier, tags, clusterParameterGroupName);

    tasks.addAll(
        this.createWriterInstanceAndWaitTasks(
            name, identifier, clusterIdentifier, tags, instanceParameterGroupName));
    tasks.addAll(
        this.createReaderInstancesAndWaitTasks(
            name, identifier, clusterIdentifier, tags, instanceParameterGroupName));

    ApplicationUtil.runOnExecutorService(tasks);

    log.info("MySQL cluster deployment completed successfully");
  }

  private List<String> createParameterGroups(
      String name, String identifier, Map<String, String> tags) {
    String clusterParameterGroupName =
        this.deployConfig.getClusterParameterGroupName() != null
            ? this.deployConfig.getClusterParameterGroupName()
            : Application.getState().getClusterParameterGroupName();
    if (clusterParameterGroupName == null) {
      clusterParameterGroupName =
          ApplicationUtil.joinByHyphen(name, Constants.CLUSTER_PARAMETER_GROUP_SUFFIX, identifier);
      log.info("Creating cluster parameter group: {}", clusterParameterGroupName);
      this.rdsClient.createDBClusterParameterGroup(
          clusterParameterGroupName, this.deployConfig.getVersion(), tags);
      ClusterParameterGroupConfig clusterParameterGroupConfig =
          this.deployConfig.getClusterParameterGroupConfig();
      if (clusterParameterGroupConfig != null) {
        log.info("Configuring cluster parameter group: {}", clusterParameterGroupName);
        this.rdsClient.configureDBClusterParameters(
            clusterParameterGroupName, clusterParameterGroupConfig);
      }
      Application.getState().setClusterParameterGroupName(clusterParameterGroupName);
    }
    String instanceParameterGroupName =
        this.deployConfig.getInstanceConfig() != null
                && this.deployConfig.getInstanceConfig().getInstanceParameterGroupName() != null
            ? this.deployConfig.getInstanceConfig().getInstanceParameterGroupName()
            : Application.getState().getInstanceParameterGroupName();
    if (instanceParameterGroupName == null) {
      instanceParameterGroupName =
          ApplicationUtil.joinByHyphen(name, Constants.INSTANCE_PARAMETER_GROUP_SUFFIX, identifier);
      log.info("Creating instance parameter group: {}", instanceParameterGroupName);
      this.rdsClient.createDBInstanceParameterGroup(
          instanceParameterGroupName, this.deployConfig.getVersion(), tags);
      InstanceConfig instanceConfig = this.deployConfig.getInstanceConfig();
      InstanceParameterGroupConfig instanceParameterGroupConfig =
          instanceConfig != null ? instanceConfig.getInstanceParameterGroupConfig() : null;
      if (instanceParameterGroupConfig != null) {
        log.info("Configuring instance parameter group: {}", instanceParameterGroupName);
        this.rdsClient.configureDBInstanceParameters(
            instanceParameterGroupName, instanceParameterGroupConfig);
      }
      Application.getState().setInstanceParameterGroupName(instanceParameterGroupName);
    }
    return List.of(clusterParameterGroupName, instanceParameterGroupName);
  }

  private String createClusterAndWait(
      String name, String identifier, Map<String, String> tags, String clusterParameterGroupName) {
    String clusterIdentifier = Application.getState().getClusterIdentifier();
    if (clusterIdentifier == null) {
      clusterIdentifier = ApplicationUtil.joinByHyphen(name, identifier);
      List<String> endpoints;
      String snapshotIdentifier = this.deployConfig.getSnapshotIdentifier();
      if (snapshotIdentifier != null) {
        log.info(
            "Restoring DB cluster {} from snapshot: {}", clusterIdentifier, snapshotIdentifier);
        endpoints =
            this.rdsClient.restoreDBClusterFromSnapshot(
                clusterIdentifier,
                tags,
                this.deployConfig,
                clusterParameterGroupName,
                this.rdsData);
      } else {
        log.info("Creating DB cluster: {}", clusterIdentifier);
        endpoints =
            this.rdsClient.createDBClusterFromScratch(
                clusterIdentifier,
                clusterParameterGroupName,
                tags,
                this.deployConfig,
                this.rdsData);
      }
      Application.getState().setClusterIdentifier(clusterIdentifier);
      Application.getState().setWriterEndpoint(endpoints.get(0));
      Application.getState().setReaderEndpoint(endpoints.get(1));
      log.info("Waiting for DB cluster to become available: {}", clusterIdentifier);
      this.rdsClient.waitUntilDBClusterAvailable(clusterIdentifier);
      log.info("DB cluster is now available: {}", clusterIdentifier);
    }
    return clusterIdentifier;
  }

  private List<Callable<Void>> createWriterInstanceAndWaitTasks(
      String name,
      String identifier,
      String clusterIdentifier,
      Map<String, String> tags,
      String instanceParameterGroupName) {
    List<Callable<Void>> tasks = new ArrayList<>();

    if (Application.getState().getWriterInstanceIdentifier() == null) {
      String instanceId = ApplicationUtil.generateRandomId(Constants.RANDOM_ID_LENGTH);
      String writerInstanceIdentifier = ApplicationUtil.joinByHyphen(name, instanceId, identifier);
      log.info("Creating DB writer instance: {}", writerInstanceIdentifier);
      this.rdsClient.createDBInstance(
          writerInstanceIdentifier,
          clusterIdentifier,
          instanceParameterGroupName,
          tags,
          this.deployConfig.getWriter().getInstanceType(),
          this.deployConfig.getWriter().getPromotionTier(),
          this.deployConfig.getInstanceConfig());
      Application.getState().setWriterInstanceIdentifier(writerInstanceIdentifier);
      tasks.add(
          () -> {
            log.info(
                "Waiting for DB writer instance to become available: {}", writerInstanceIdentifier);
            this.rdsClient.waitUntilDBInstanceAvailable(writerInstanceIdentifier);
            log.info("DB writer instance is now available: {}", writerInstanceIdentifier);
            return null;
          });
    }
    return tasks;
  }

  private List<Callable<Void>> createReaderInstancesAndWaitTasks(
      String name,
      String identifier,
      String clusterIdentifier,
      Map<String, String> tags,
      String instanceParameterGroupName) {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (this.deployConfig.getReaders() != null && !this.deployConfig.getReaders().isEmpty()) {
      for (int i = 0; i < this.deployConfig.getReaders().size(); i++) {
        String instanceType = this.deployConfig.getReaders().get(i).getInstanceType();

        List<String> existingInstances =
            Application.getState().getReaderInstanceIdentifiers().get(instanceType);
        int stateInstanceCount = existingInstances != null ? existingInstances.size() : 0;

        for (int j = stateInstanceCount;
            j < this.deployConfig.getReaders().get(i).getInstanceCount();
            j++) {
          String instanceId = ApplicationUtil.generateRandomId(Constants.RANDOM_ID_LENGTH);
          final String readerInstanceIdentifier =
              ApplicationUtil.joinByHyphen(name, instanceId, identifier);
          log.info("Creating MySQL reader instance: {}", readerInstanceIdentifier);
          this.rdsClient.createDBInstance(
              readerInstanceIdentifier,
              clusterIdentifier,
              instanceParameterGroupName,
              tags,
              instanceType,
              this.deployConfig.getReaders().get(i).getPromotionTier(),
              this.deployConfig.getInstanceConfig());
          Application.getState()
              .getReaderInstanceIdentifiers()
              .computeIfAbsent(instanceType, k -> new ArrayList<>())
              .add(readerInstanceIdentifier);

          tasks.add(
              () -> {
                log.info(
                    "Waiting for DB reader instance to become available: {}",
                    readerInstanceIdentifier);
                this.rdsClient.waitUntilDBInstanceAvailable(readerInstanceIdentifier);
                log.info("DB reader instance is now available: {}", readerInstanceIdentifier);
                return null;
              });
        }
      }
    }

    return tasks;
  }

  public void addReaders() {
    String name =
        ApplicationUtil.joinByHyphen(
            this.componentMetadata.getComponentName(), this.componentMetadata.getEnvName());
    String identifier = Application.getState().getIdentifier();
    String clusterIdentifier = Application.getState().getClusterIdentifier();
    String instanceParameterGroupName =
        this.deployConfig.getInstanceConfig() != null
                && this.deployConfig.getInstanceConfig().getInstanceParameterGroupName() != null
            ? this.deployConfig.getInstanceConfig().getInstanceParameterGroupName()
            : Application.getState().getInstanceParameterGroupName();
    Map<String, String> tags =
        ApplicationUtil.merge(
            List.of(
                this.deployConfig.getTags(),
                this.awsAccountData.getTags(),
                Constants.COMPONENT_TAGS));

    List<Callable<Void>> tasks = new ArrayList<>();
    for (ReaderConfig readerConfig : this.addReadersConfig.getReaders()) {

      for (int j = 0; j < readerConfig.getInstanceCount(); j++) {
        String instanceId = ApplicationUtil.generateRandomId(Constants.RANDOM_ID_LENGTH);
        final String readerInstanceIdentifier =
            ApplicationUtil.joinByHyphen(name, instanceId, identifier);
        log.info("Creating DB reader instance: {}", readerInstanceIdentifier);
        this.rdsClient.createDBInstance(
            readerInstanceIdentifier,
            clusterIdentifier,
            instanceParameterGroupName,
            tags,
            readerConfig.getInstanceType(),
            readerConfig.getPromotionTier(),
            this.deployConfig.getInstanceConfig());

        tasks.add(
            () -> {
              log.info(
                  "Waiting for DB reader instance to become available: {}",
                  readerInstanceIdentifier);
              this.rdsClient.waitUntilDBInstanceAvailable(readerInstanceIdentifier);
              log.info("DB reader instance is now available: {}", readerInstanceIdentifier);
              return null;
            });
      }
    }
    ApplicationUtil.runOnExecutorService(tasks);
    log.info("MySQL add reader instances operation completed successfully");
  }

  public void removeReaders() {
    List<Callable<Void>> tasks = new ArrayList<>();

    Map<String, List<String>> readerInstances =
        Application.getState().getReaderInstanceIdentifiers();

    for (ReaderConfig removeConfig : this.removeReadersConfig.getReaders()) {
      String instanceType = removeConfig.getInstanceType();
      Integer removeCount = removeConfig.getInstanceCount();

      List<String> existingInstances =
          readerInstances.computeIfAbsent(
              instanceType,
              __ -> {
                throw new GenericApplicationException(
                    ApplicationError.CONSTRAINT_VIOLATION,
                    String.format(
                        "Instance type '%s' does not exist in current deployment", instanceType));
              });

      if (removeCount > existingInstances.size()) {
        throw new GenericApplicationException(
            ApplicationError.CONSTRAINT_VIOLATION,
            String.format(
                "Cannot remove %d instances of type '%s'. Only %d instances exist",
                removeCount, instanceType, existingInstances.size()));
      }

      for (int i = 0; i < removeCount; i++) {
        final String instanceToRemove = existingInstances.get(existingInstances.size() - i - 1);
        log.info("Removing DB reader instance: {}", instanceToRemove);
        this.rdsClient.deleteDBInstance(instanceToRemove, this.deployConfig.getDeletionConfig());

        tasks.add(
            () -> {
              log.info("Waiting for DB reader instance to be deleted: {}", instanceToRemove);
              this.rdsClient.waitUntilDBInstanceDeleted(instanceToRemove);
              log.info("DB reader instance has been deleted: {}", instanceToRemove);
              return null;
            });
      }
    }

    ApplicationUtil.runOnExecutorService(tasks);
    log.info("MySQL remove reader instances operation completed successfully");
  }

  @SneakyThrows
  private void waitUntilDBClusterFailover(String clusterIdentifier) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() < startTime + Constants.DB_WAIT_RETRY_TIMEOUT.toMillis()) {
      String status = this.rdsClient.getDBCluster(clusterIdentifier).status();
      log.debug("DB cluster {} status: {}", clusterIdentifier, status);
      if ("failing-over".equals(status)) {
        return;
      }
      Thread.sleep(Constants.DB_WAIT_RETRY_INTERVAL.toMillis());
    }
    throw new GenericApplicationException(
        ApplicationError.DB_WAIT_TIMEOUT, "cluster", clusterIdentifier, "failover");
  }

  public void failover() {
    String clusterIdentifier = Application.getState().getClusterIdentifier();
    String readerInstanceIdentifier = this.failoverConfig.getReaderInstanceIdentifier();
    log.info("Failing over DB reader instance as writer: {}", readerInstanceIdentifier);
    this.rdsClient.failoverDBCluster(clusterIdentifier, readerInstanceIdentifier);
    log.info("Waiting for DB cluster to become failover: {}", clusterIdentifier);
    this.waitUntilDBClusterFailover(clusterIdentifier);
    log.info("DB cluster has entered failover state: {}", clusterIdentifier);
    log.info("Waiting for DB cluster to become available: {}", clusterIdentifier);
    this.rdsClient.waitUntilDBClusterAvailable(clusterIdentifier);
    log.info("DB cluster is now available: {}", clusterIdentifier);
    log.info("MySQL failover operation completed successfully");
  }

  public void reboot() {
    List<Callable<Void>> tasks = new ArrayList<>();
    for (String instanceIdentifier : this.rebootConfig.getInstanceIdentifiers()) {
      log.info("Rebooting DB instance: {}", instanceIdentifier);
      this.rdsClient.rebootDBInstance(instanceIdentifier);
      tasks.add(
          () -> {
            log.info("Waiting for DB instance to become available: {}", instanceIdentifier);
            this.rdsClient.waitUntilDBInstanceAvailable(instanceIdentifier);
            log.info("DB instance is now available: {}", instanceIdentifier);
            return null;
          });
    }
    ApplicationUtil.runOnExecutorService(tasks);
    log.info("MySQL reboot operation completed successfully");
  }

  public void updateCluster() {
    String clusterIdentifier = Application.getState().getClusterIdentifier();

    String updatedClusterParameterGroupName = null;
    String updatedInstanceParameterGroupName = null;
    String updatedEngineVersion = null;

    if (this.updateClusterConfig.getClusterParameterGroupName() != null
        || this.updateClusterConfig.getClusterParameterGroupConfig() != null) {
      updatedClusterParameterGroupName = this.handleClusterParameterGroupUpdate(clusterIdentifier);
    }
    if (this.updateClusterConfig.getInstanceConfig() != null
        && (this.updateClusterConfig.getInstanceConfig().getInstanceParameterGroupName() != null
            || this.updateClusterConfig.getInstanceConfig().getInstanceParameterGroupConfig()
                != null)) {
      updatedInstanceParameterGroupName = this.handleInstanceParameterGroupUpdate();
    }

    if (this.updateClusterConfig.getEngineVersion() != null) {
      updatedEngineVersion =
          Application.getState().getDeployConfig().getVersion()
              + ".mysql_aurora."
              + this.updateClusterConfig.getEngineVersion();
    }

    log.info("Updating cluster: {}", clusterIdentifier);
    this.rdsClient.updateDBCluster(
        clusterIdentifier,
        this.updateClusterConfig,
        updatedEngineVersion,
        updatedClusterParameterGroupName);
    if (this.updateClusterConfig.getApplyImmediately()) {
      log.info("Waiting for cluster to become available after update: {}", clusterIdentifier);
      this.rdsClient.waitUntilDBClusterAvailable(clusterIdentifier);
      log.info("Cluster is now available: {}", clusterIdentifier);
    }

    if (this.updateClusterConfig.getInstanceConfig() != null) {
      List<Callable<Void>> tasks = new ArrayList<>();

      String writerInstanceIdentifier = Application.getState().getWriterInstanceIdentifier();
      if (writerInstanceIdentifier != null) {
        log.info("Updating writer instance: {}", writerInstanceIdentifier);
        this.rdsClient.updateDBInstance(
            writerInstanceIdentifier,
            this.updateClusterConfig.getInstanceConfig(),
            updatedInstanceParameterGroupName,
            this.updateClusterConfig.getApplyImmediately());
        if (this.updateClusterConfig.getApplyImmediately()) {
          tasks.add(
              () -> {
                log.info(
                    "Waiting for writer instance to become available: {}",
                    writerInstanceIdentifier);
                this.rdsClient.waitUntilDBInstanceAvailable(writerInstanceIdentifier);
                log.info("Writer instance update completed: {}", writerInstanceIdentifier);
                return null;
              });
        }
      }

      for (Map.Entry<String, List<String>> entry :
          Application.getState().getReaderInstanceIdentifiers().entrySet()) {
        for (String readerInstanceIdentifier : entry.getValue()) {
          log.info("Updating reader instance: {}", readerInstanceIdentifier);
          this.rdsClient.updateDBInstance(
              readerInstanceIdentifier,
              this.updateClusterConfig.getInstanceConfig(),
              updatedInstanceParameterGroupName,
              this.updateClusterConfig.getApplyImmediately());
          if (this.updateClusterConfig.getApplyImmediately()) {
            tasks.add(
                () -> {
                  log.info(
                      "Waiting for reader instance to become available: {}",
                      readerInstanceIdentifier);
                  this.rdsClient.waitUntilDBInstanceAvailable(readerInstanceIdentifier);
                  log.info("Reader instance update completed: {}", readerInstanceIdentifier);
                  return null;
                });
          }
        }
      }

      ApplicationUtil.runOnExecutorService(tasks);
    }

    if (!this.updateClusterConfig.getTags().isEmpty()) {
      this.handleTagUpdates(clusterIdentifier);
    }

    log.info("MySQL update cluster operation completed successfully");
  }

  private String handleClusterParameterGroupUpdate(String clusterIdentifier) {
    if (Application.getState().getDeployConfig().getClusterParameterGroupName() != null) {
      if (this.updateClusterConfig.getClusterParameterGroupName() != null) {
        log.info(
            "Switching cluster parameter group from {} to {}",
            Application.getState().getDeployConfig().getClusterParameterGroupName(),
            this.updateClusterConfig.getClusterParameterGroupName());
        return this.updateClusterConfig.getClusterParameterGroupName();
      } else {
        throw new GenericApplicationException(
            ApplicationError.CANNOT_MODIFY_PARAMETER_GROUP_CONFIG);
      }
    } else {
      if (this.updateClusterConfig.getClusterParameterGroupConfig() != null) {
        log.info(
            "Updating cluster parameter group configuration: {}",
            Application.getState().getClusterParameterGroupName());
        this.rdsClient.configureDBClusterParameters(
            Application.getState().getClusterParameterGroupName(),
            this.updateClusterConfig.getClusterParameterGroupConfig());
        return null;
      } else if (this.updateClusterConfig.getClusterParameterGroupName() != null
          && !this.updateClusterConfig
              .getClusterParameterGroupName()
              .equals(Application.getState().getClusterParameterGroupName())) {
        log.info(
            "Switching cluster parameter group from {} to {}",
            Application.getState().getClusterParameterGroupName(),
            this.updateClusterConfig.getClusterParameterGroupName());
        return this.updateClusterConfig.getClusterParameterGroupName();
      }
    }

    return null;
  }

  private String handleInstanceParameterGroupUpdate() {
    String oldParameterGroupName =
        Application.getState().getDeployConfig().getInstanceConfig() != null
            ? Application.getState()
                .getDeployConfig()
                .getInstanceConfig()
                .getInstanceParameterGroupName()
            : null;
    String newParameterGroupName =
        this.updateClusterConfig.getInstanceConfig() != null
            ? this.updateClusterConfig.getInstanceConfig().getInstanceParameterGroupName()
            : null;
    InstanceParameterGroupConfig newParameterGroupConfig =
        this.updateClusterConfig.getInstanceConfig() != null
            ? this.updateClusterConfig.getInstanceConfig().getInstanceParameterGroupConfig()
            : null;

    if (oldParameterGroupName != null) {
      if (newParameterGroupName != null) {
        log.info(
            "Switching instance parameter group from {} to {}",
            oldParameterGroupName,
            newParameterGroupName);
        return newParameterGroupName;
      } else {
        throw new GenericApplicationException(
            ApplicationError.CANNOT_MODIFY_PARAMETER_GROUP_CONFIG);
      }
    } else {
      if (newParameterGroupConfig != null) {
        log.info(
            "Updating instance parameter group configuration: {}",
            Application.getState().getInstanceParameterGroupName());
        this.rdsClient.configureDBInstanceParameters(
            Application.getState().getInstanceParameterGroupName(), newParameterGroupConfig);
        return null;
      } else if (newParameterGroupName != null
          && !newParameterGroupName.equals(
              Application.getState().getInstanceParameterGroupName())) {
        log.info(
            "Switching instance parameter group from {} to {}",
            Application.getState().getInstanceParameterGroupName(),
            newParameterGroupName);
        return newParameterGroupName;
      }
    }

    return null;
  }

  private void handleTagUpdates(String clusterIdentifier) {
    Map<String, String> tags = this.updateClusterConfig.getTags();

    String clusterArn = this.rdsClient.getDBCluster(clusterIdentifier).dbClusterArn();
    this.rdsClient.updateTagsForResource(clusterArn, tags);

    String writerInstanceIdentifier = Application.getState().getWriterInstanceIdentifier();
    if (writerInstanceIdentifier != null) {
      String writerArn = this.rdsClient.getDBInstance(writerInstanceIdentifier).dbInstanceArn();
      this.rdsClient.updateTagsForResource(writerArn, tags);
    }

    Map<String, List<String>> readerInstanceIdentifiers =
        Application.getState().getReaderInstanceIdentifiers();
    for (Map.Entry<String, List<String>> entry : readerInstanceIdentifiers.entrySet()) {
      for (String readerInstanceIdentifier : entry.getValue()) {
        String readerArn = this.rdsClient.getDBInstance(readerInstanceIdentifier).dbInstanceArn();
        this.rdsClient.updateTagsForResource(readerArn, tags);
      }
    }

    String clusterParameterGroupName = Application.getState().getClusterParameterGroupName();
    if (clusterParameterGroupName != null) {
      String clusterParamGroupArn =
          this.rdsClient
              .getDBClusterParameterGroup(clusterParameterGroupName)
              .dbClusterParameterGroupArn();
      this.rdsClient.updateTagsForResource(clusterParamGroupArn, tags);
    }

    String instanceParameterGroupName = Application.getState().getInstanceParameterGroupName();
    if (instanceParameterGroupName != null) {
      String instanceParamGroupArn =
          this.rdsClient.getDBParameterGroup(instanceParameterGroupName).dbParameterGroupArn();
      this.rdsClient.updateTagsForResource(instanceParamGroupArn, tags);
    }
  }

  public void undeploy() {
    List<Callable<Void>> tasks = new ArrayList<>();

    tasks.addAll(this.deleteReaderInstancesAndWaitTasks());
    tasks.addAll(this.deleteWriterInstanceAndWaitTasks());

    ApplicationUtil.runOnExecutorService(tasks);

    this.deleteClusterAndWait();

    this.deleteParameterGroups();

    log.info("MySQL cluster undeployment completed successfully");
  }

  private List<Callable<Void>> deleteReaderInstancesAndWaitTasks() {
    List<Callable<Void>> tasks = new ArrayList<>();
    List<Map.Entry<String, List<String>>> readerInstancesToDelete =
        new ArrayList<>(Application.getState().getReaderInstanceIdentifiers().entrySet());
    for (Map.Entry<String, List<String>> entry : readerInstancesToDelete) {
      List<String> readerInstanceIdentifiers = entry.getValue();
      String key = entry.getKey();
      for (String readerInstanceIdentifier : readerInstanceIdentifiers) {
        log.info("Deleting DB reader instance: {}", readerInstanceIdentifier);
        this.rdsClient.deleteDBInstance(
            readerInstanceIdentifier, this.deployConfig.getDeletionConfig());
        tasks.add(
            () -> {
              log.info(
                  "Waiting for DB reader instance to become deleted: {}", readerInstanceIdentifier);
              this.rdsClient.waitUntilDBInstanceDeleted(readerInstanceIdentifier);
              log.info("DB reader instance is now deleted: {}", readerInstanceIdentifier);
              Application.getState()
                  .getReaderInstanceIdentifiers()
                  .get(key)
                  .remove(readerInstanceIdentifier);
              if (Application.getState().getReaderInstanceIdentifiers().get(key).isEmpty()) {
                Application.getState().getReaderInstanceIdentifiers().remove(key);
              }
              return null;
            });
      }
    }
    return tasks;
  }

  private List<Callable<Void>> deleteWriterInstanceAndWaitTasks() {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (Application.getState().getWriterInstanceIdentifier() != null) {
      log.info(
          "Deleting DB writer instance: {}", Application.getState().getWriterInstanceIdentifier());
      this.rdsClient.deleteDBInstance(
          Application.getState().getWriterInstanceIdentifier(),
          this.deployConfig != null ? this.deployConfig.getDeletionConfig() : null);
      tasks.add(
          () -> {
            log.info(
                "Waiting for DB writer instance to become deleted: {}",
                Application.getState().getWriterInstanceIdentifier());
            this.rdsClient.waitUntilDBInstanceDeleted(
                Application.getState().getWriterInstanceIdentifier());
            log.info(
                "DB writer instance is now deleted: {}",
                Application.getState().getWriterInstanceIdentifier());
            Application.getState().setWriterInstanceIdentifier(null);
            return null;
          });
    }
    return tasks;
  }

  private void deleteClusterAndWait() {
    if (Application.getState().getClusterIdentifier() != null) {
      log.info("Deleting DB cluster: {}", Application.getState().getClusterIdentifier());
      this.rdsClient.deleteDBCluster(
          Application.getState().getClusterIdentifier(),
          this.deployConfig != null ? this.deployConfig.getDeletionConfig() : null);
      log.info(
          "Waiting for DB cluster to become deleted: {}",
          Application.getState().getClusterIdentifier());
      this.rdsClient.waitUntilDBClusterDeleted(Application.getState().getClusterIdentifier());
      log.info("DB cluster is now deleted: {}", Application.getState().getClusterIdentifier());
      Application.getState().setClusterIdentifier(null);
      Application.getState().setWriterEndpoint(null);
      Application.getState().setReaderEndpoint(null);
    }
  }

  private void deleteParameterGroups() {
    if (Application.getState().getClusterParameterGroupName() != null) {
      log.info(
          "Deleting DB cluster parameter group: {}",
          Application.getState().getClusterParameterGroupName());
      this.rdsClient.deleteDBClusterParameterGroup(
          Application.getState().getClusterParameterGroupName());
      Application.getState().setClusterParameterGroupName(null);
    }
    if (Application.getState().getInstanceParameterGroupName() != null) {
      log.info(
          "Deleting DB instance parameter group: {}",
          Application.getState().getInstanceParameterGroupName());
      this.rdsClient.deleteDBParameterGroup(Application.getState().getInstanceParameterGroupName());
      Application.getState().setInstanceParameterGroupName(null);
    }
  }
}
