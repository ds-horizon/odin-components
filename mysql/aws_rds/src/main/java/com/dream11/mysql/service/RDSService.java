package com.dream11.mysql.service;

import com.dream11.mysql.Application;
import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.config.metadata.ComponentMetadata;
import com.dream11.mysql.config.metadata.aws.AwsAccountData;
import com.dream11.mysql.config.metadata.aws.RDSData;
import com.dream11.mysql.config.user.AddReadersConfig;
import com.dream11.mysql.config.user.ClusterParameterGroupConfig;
import com.dream11.mysql.config.user.DeployConfig;
import com.dream11.mysql.config.user.InstanceConfig;
import com.dream11.mysql.config.user.InstanceParameterGroupConfig;
import com.dream11.mysql.config.user.ReaderConfig;
import com.dream11.mysql.config.user.RemoveReadersConfig;
import com.dream11.mysql.constant.Constants;
import com.dream11.mysql.error.ApplicationError;
import com.dream11.mysql.exception.GenericApplicationException;
import com.dream11.mysql.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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

  public void deploy() {
    List<Callable<Void>> tasks = new ArrayList<>();
    String identifier = Application.getState().getIdentifier();
    if (identifier == null) {
      identifier = ApplicationUtil.generateRandomId(4);
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
        this.createWriterInstanceAndWaitTaks(
            name, identifier, clusterIdentifier, tags, instanceParameterGroupName));
    tasks.addAll(
        this.createReaderInstancesAndWaitTaks(
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
      clusterIdentifier = ApplicationUtil.joinByHyphen(name, Constants.CLUSTER_SUFFIX, identifier);
      List<String> endpoints;
      String snapshotIdentifier = this.deployConfig.getSnapshotIdentifier();
      if (snapshotIdentifier != null) {
        log.info(
            "Restoring DB cluster {} from snapshot: {}", clusterIdentifier, snapshotIdentifier);
        endpoints =
            this.rdsClient.restoreDBClusterFromSnapshot(
                clusterIdentifier,
                snapshotIdentifier,
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

  private List<Callable<Void>> createWriterInstanceAndWaitTaks(
      String name,
      String identifier,
      String clusterIdentifier,
      Map<String, String> tags,
      String instanceParameterGroupName) {
    List<Callable<Void>> tasks = new ArrayList<>();

    if (Application.getState().getWriterInstanceIdentifier() == null) {
      String writerInstanceIdentifier =
          ApplicationUtil.joinByHyphen(name, Constants.WRITER_INSTANCE_SUFFIX, identifier);
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

  private List<Callable<Void>> createReaderInstancesAndWaitTaks(
      String name,
      String identifier,
      String clusterIdentifier,
      Map<String, String> tags,
      String instanceParameterGroupName) {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (this.deployConfig.getReaders() != null && !this.deployConfig.getReaders().isEmpty()) {
      if (Application.getState().getReaderInstanceIdentifiers() == null) {
        Application.getState().setReaderInstanceIdentifiers(new HashMap<>());
      }

      for (int i = 0; i < this.deployConfig.getReaders().size(); i++) {
        String instanceType = this.deployConfig.getReaders().get(i).getInstanceType();
        Integer promotionTier = this.deployConfig.getReaders().get(i).getPromotionTier();
        Integer instanceCount = this.deployConfig.getReaders().get(i).getInstanceCount();

        List<String> existingInstances =
            Application.getState().getReaderInstanceIdentifiers().get(instanceType);
        Integer stateInstanceCount = existingInstances != null ? existingInstances.size() : 0;

        for (int j = stateInstanceCount; j < instanceCount; j++) {
          String instanceId = ApplicationUtil.generateRandomId(4);
          String readerInstanceIdentifier =
              ApplicationUtil.joinByHyphen(
                  name, Constants.READER_INSTANCE_SUFFIX, instanceId, identifier);
          log.info("Creating MySQL reader instance: {}", readerInstanceIdentifier);
          this.rdsClient.createDBInstance(
              readerInstanceIdentifier,
              clusterIdentifier,
              instanceParameterGroupName,
              tags,
              instanceType,
              promotionTier,
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
    DeployConfig deployConfig = Application.getState().getDeployConfig();
    String instanceParameterGroupName =
        deployConfig.getInstanceConfig() != null
                && deployConfig.getInstanceConfig().getInstanceParameterGroupName() != null
            ? deployConfig.getInstanceConfig().getInstanceParameterGroupName()
            : Application.getState().getInstanceParameterGroupName();
    Map<String, String> tags =
        ApplicationUtil.merge(
            List.of(
                deployConfig.getTags(), this.awsAccountData.getTags(), Constants.COMPONENT_TAGS));

    List<Callable<Void>> tasks = new ArrayList<>();
    if (this.addReadersConfig.getReaders() != null
        && !this.addReadersConfig.getReaders().isEmpty()) {
      if (Application.getState().getReaderInstanceIdentifiers() == null) {
        Application.getState().setReaderInstanceIdentifiers(new HashMap<>());
      }

      Map<String, ReaderConfig> readersMap = new HashMap<>();
      deployConfig.getReaders().forEach(reader -> readersMap.put(reader.getInstanceType(), reader));

      for (int i = 0; i < this.addReadersConfig.getReaders().size(); i++) {
        String instanceType = this.addReadersConfig.getReaders().get(i).getInstanceType();
        Integer promotionTier = this.addReadersConfig.getReaders().get(i).getPromotionTier();
        Integer instanceCount = this.addReadersConfig.getReaders().get(i).getInstanceCount();

        for (int j = 0; j < instanceCount; j++) {
          String instanceId = ApplicationUtil.generateRandomId(4);
          String readerInstanceIdentifier =
              ApplicationUtil.joinByHyphen(
                  name, Constants.READER_INSTANCE_SUFFIX, instanceId, identifier);
          log.info("Creating DB reader instance: {}", readerInstanceIdentifier);
          this.rdsClient.createDBInstance(
              readerInstanceIdentifier,
              clusterIdentifier,
              instanceParameterGroupName,
              tags,
              instanceType,
              promotionTier,
              deployConfig.getInstanceConfig());
          Application.getState()
              .getReaderInstanceIdentifiers()
              .computeIfAbsent(instanceType, k -> new ArrayList<>())
              .add(readerInstanceIdentifier);
          if (readersMap.containsKey(instanceType)) {
            readersMap
                .get(instanceType)
                .setInstanceCount(readersMap.get(instanceType).getInstanceCount() + 1);
          } else {
            readersMap.put(
                instanceType,
                ReaderConfig.builder()
                    .instanceCount(1)
                    .instanceType(instanceType)
                    .promotionTier(promotionTier)
                    .build());
          }

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
      deployConfig.setReaders(new ArrayList<>(readersMap.values()));
    }
    ApplicationUtil.runOnExecutorService(tasks);
    log.info("MySQL add reader instances operation completed successfully");
  }

  public void removeReaders() {
    DeployConfig deployConfig = Application.getState().getDeployConfig();
    List<Callable<Void>> tasks = new ArrayList<>();

    if (this.removeReadersConfig.getReaders() != null
        && !this.removeReadersConfig.getReaders().isEmpty()) {
      Map<String, List<String>> readerInstances =
          Application.getState().getReaderInstanceIdentifiers();

      if (readerInstances == null || readerInstances.isEmpty()) {
        throw new GenericApplicationException(
            ApplicationError.CONSTRAINT_VIOLATION, "No DB reader instances exist to remove");
      }

      Map<String, ReaderConfig> readersMap = new HashMap<>();
      deployConfig.getReaders().forEach(reader -> readersMap.put(reader.getInstanceType(), reader));

      for (ReaderConfig removeConfig : this.removeReadersConfig.getReaders()) {
        String instanceType = removeConfig.getInstanceType();
        Integer removeCount = removeConfig.getInstanceCount();

        List<String> existingInstances = readerInstances.get(instanceType);

        if (existingInstances == null || existingInstances.isEmpty()) {
          throw new GenericApplicationException(
              ApplicationError.CONSTRAINT_VIOLATION,
              String.format(
                  "Instance type '%s' does not exist in current deployment", instanceType));
        }

        if (removeCount > existingInstances.size()) {
          throw new GenericApplicationException(
              ApplicationError.CONSTRAINT_VIOLATION,
              String.format(
                  "Cannot remove %d instances of type '%s'. Only %d instances exist",
                  removeCount, instanceType, existingInstances.size()));
        }

        Integer existingInstanceCount = existingInstances.size();

        for (int i = 0; i < removeCount; i++) {
          String instanceToRemove = existingInstances.get(existingInstanceCount - 1);
          log.info("Removing DB reader instance: {}", instanceToRemove);
          this.rdsClient.deleteDBInstance(instanceToRemove, deployConfig.getDeletionConfig());
          existingInstances.remove(existingInstanceCount - 1);
          if (existingInstances.isEmpty()) {
            readerInstances.remove(instanceType);
          }
          readersMap
              .get(instanceType)
              .setInstanceCount(readersMap.get(instanceType).getInstanceCount() - 1);
          if (readersMap.get(instanceType).getInstanceCount() <= 0) {
            readersMap.remove(instanceType);
          }

          tasks.add(
              () -> {
                log.info("Waiting for DB reader instance to be deleted: {}", instanceToRemove);
                this.rdsClient.waitUntilDBInstanceDeleted(instanceToRemove);
                log.info("DB reader instance has been deleted: {}", instanceToRemove);
                return null;
              });
        }
      }

      deployConfig.setReaders(new ArrayList<>(readersMap.values()));
    }

    ApplicationUtil.runOnExecutorService(tasks);
    log.info("MySQL remove reader instances operation completed successfully");
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
    if (Application.getState().getReaderInstanceIdentifiers() != null) {
      List<Map.Entry<String, List<String>>> readerInstancesToDelete =
          new ArrayList<>(Application.getState().getReaderInstanceIdentifiers().entrySet());
      for (Map.Entry<String, List<String>> entry : readerInstancesToDelete) {
        List<String> readerInstanceIdentifiers = entry.getValue();
        String key = entry.getKey();
        for (String readerInstanceIdentifier : readerInstanceIdentifiers) {
          log.info("Deleting DB reader instance: {}", readerInstanceIdentifier);
          this.rdsClient.deleteDBInstance(
              readerInstanceIdentifier,
              Application.getState().getDeployConfig().getDeletionConfig());
          tasks.add(
              () -> {
                log.info(
                    "Waiting for DB reader instance to become deleted: {}",
                    readerInstanceIdentifier);
                this.rdsClient.waitUntilDBInstanceDeleted(readerInstanceIdentifier);
                log.info("DB reader instance is now deleted: {}", readerInstanceIdentifier);
                Application.getState().getReaderInstanceIdentifiers().remove(key);
                return null;
              });
        }
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
          Application.getState().getDeployConfig().getDeletionConfig());
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
          Application.getState().getDeployConfig().getDeletionConfig());
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
