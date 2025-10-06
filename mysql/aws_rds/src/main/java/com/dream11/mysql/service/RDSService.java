package com.dream11.mysql.service;

import com.dream11.mysql.Application;
import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.config.metadata.ComponentMetadata;
import com.dream11.mysql.config.metadata.aws.AwsAccountData;
import com.dream11.mysql.config.metadata.aws.RDSData;
import com.dream11.mysql.config.user.DeployConfig;
import com.dream11.mysql.constant.Constants;
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

  public void deployService() {
    List<Callable<Void>> tasks = new ArrayList<>();

    log.info("Starting with mysql deployment");
    String name =
        ApplicationUtil.joinByHyphen(
            this.componentMetadata.getComponentName(), this.componentMetadata.getEnvName());
    String randomId = ApplicationUtil.generateRandomId(4);

    Map<String, String> tags =
        ApplicationUtil.merge(List.of(this.deployConfig.getTags(), this.awsAccountData.getTags(), Constants.COMPONENT_TAGS));

    String clusterIdentifier = this.createClusterAndWait(name, randomId, tags);

    tasks.addAll(this.createWriterInstanceAndWaitTaks(name, randomId, clusterIdentifier, tags));
    tasks.addAll(this.createReaderInstancesAndWaitTaks(name, randomId, clusterIdentifier, tags));

    ApplicationUtil.runOnExecutorService(tasks);

    log.info("MySQL cluster deployment completed successfully");
  }

  private String createClusterAndWait(String name, String randomId, Map<String, String> tags) {
    String clusterParameterGroupName =
        this.deployConfig.getClusterParameterGroupName() != null
            ? this.deployConfig.getClusterParameterGroupName()
            : Application.getState().getClusterParameterGroupName();
    if (clusterParameterGroupName == null) {
      clusterParameterGroupName =
          ApplicationUtil.joinByHyphen(name, Constants.CLUSTER_PARAMETER_GROUP_SUFFIX, randomId);
      this.rdsClient.createDBClusterParameterGroup(clusterParameterGroupName, tags, this.deployConfig);
      Application.getState().setClusterParameterGroupName(clusterParameterGroupName);
    }

    String clusterIdentifier = Application.getState().getClusterIdentifier();
    if (clusterIdentifier == null) {
      clusterIdentifier = ApplicationUtil.joinByHyphen(name, Constants.CLUSTER_SUFFIX, randomId);
      List<String> endpoints =
          this.rdsClient.createDBCluster(
              clusterIdentifier, clusterParameterGroupName, tags, this.deployConfig, this.rdsData);
      Application.getState().setClusterIdentifier(clusterIdentifier);
      Application.getState().setWriterEndpoint(endpoints.get(0));
      Application.getState().setReaderEndpoint(endpoints.get(1));
      this.rdsClient.waitUntilDBClusterAvailable(clusterIdentifier);
    }
    return clusterIdentifier;
  }

  private List<Callable<Void>> createWriterInstanceAndWaitTaks(
      String name, String randomId, String clusterIdentifier, Map<String, String> tags) {
    List<Callable<Void>> tasks = new ArrayList<>();
    String writerInstanceParameterGroupName =
        this.deployConfig.getWriter().getInstanceParameterGroupName() != null
            ? this.deployConfig.getWriter().getInstanceParameterGroupName()
            : Application.getState().getWriterInstanceParameterGroupName();
    if (writerInstanceParameterGroupName == null) {
      writerInstanceParameterGroupName =
          ApplicationUtil.joinByHyphen(name, Constants.WRITER_INSTANCE_PARAMETER_GROUP_SUFFIX, randomId);
      this.rdsClient.createDBInstanceParameterGroup(
          writerInstanceParameterGroupName,
          this.deployConfig.getVersion(),
          tags,
          this.deployConfig.getWriter().getInstanceParameterGroupConfig());
      Application.getState().setWriterInstanceParameterGroupName(writerInstanceParameterGroupName);
    }

    if (Application.getState().getWriterInstanceIdentifier() == null) {
      String writerInstanceIdentifier =
          ApplicationUtil.joinByHyphen(name, Constants.WRITER_INSTANCE_SUFFIX, randomId);
      this.rdsClient.createDBInstance(
          writerInstanceIdentifier,
          clusterIdentifier,
          writerInstanceParameterGroupName,
          tags,
          this.deployConfig.getWriter());
      Application.getState().setWriterInstanceIdentifier(writerInstanceIdentifier);
      tasks.add(
          () -> {
            this.rdsClient.waitUntilDBInstanceAvailable(writerInstanceIdentifier);
            return null;
          });
    }
    return tasks;
  }

  private List<Callable<Void>> createReaderInstancesAndWaitTaks(
      String name, String randomId, String clusterIdentifier, Map<String, String> tags) {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (this.deployConfig.getReaders() != null && !this.deployConfig.getReaders().isEmpty()) {
      if (Application.getState().getReaderInstanceIdentifiers() == null) {
        Application.getState().setReaderInstanceIdentifiers(new HashMap<>());
      }
      if (Application.getState().getReaderInstanceParameterGroupNames() == null) {
        Application.getState().setReaderInstanceParameterGroupNames(new HashMap<>());
      }

      for (int i = 0; i < this.deployConfig.getReaders().size(); i++) {
        String parameterGroupKey = String.valueOf(i);
        String instanceParameterGroupName =
            this.deployConfig.getReaders().get(i).getInstanceParameterGroupName() != null
                ? this.deployConfig.getReaders().get(i).getInstanceParameterGroupName()
                : Application.getState()
                    .getReaderInstanceParameterGroupNames()
                    .get(parameterGroupKey);
        if (instanceParameterGroupName == null) {
          instanceParameterGroupName =
              ApplicationUtil.joinByHyphen(
                  name, Constants.READER_INSTANCE_PARAMETER_GROUP_SUFFIX, parameterGroupKey, randomId);
          this.rdsClient.createDBInstanceParameterGroup(
              instanceParameterGroupName,
              this.deployConfig.getVersion(),
              tags,
              this.deployConfig.getReaders().get(i).getInstanceParameterGroupConfig());
          Application.getState()
              .getReaderInstanceParameterGroupNames()
              .put(parameterGroupKey, instanceParameterGroupName);
        }
        for (int j = 0; j < this.deployConfig.getReaders().get(i).getInstanceCount(); j++) {
          String instanceKey = ApplicationUtil.joinByHyphen(String.valueOf(i), String.valueOf(j));

          if (Application.getState().getReaderInstanceIdentifiers().get(instanceKey) == null) {
            String readerInstanceIdentifier =
                ApplicationUtil.joinByHyphen(
                    name, Constants.READER_INSTANCE_SUFFIX, instanceKey, randomId);
            this.rdsClient.createDBInstance(
                readerInstanceIdentifier,
                clusterIdentifier,
                instanceParameterGroupName,
                tags,
                this.deployConfig.getReaders().get(i));
            Application.getState()
                .getReaderInstanceIdentifiers()
                .put(instanceKey, readerInstanceIdentifier);

            tasks.add(
                () -> {
                  this.rdsClient.waitUntilDBInstanceAvailable(readerInstanceIdentifier);
                  return null;
                });
          }
        }
      }
    }

    return tasks;
  }

  public void undeployService() {
    List<Callable<Void>> tasks = new ArrayList<>();

    tasks.addAll(deleteReaderInstancesAndWaitTaks());
    tasks.addAll(deleteWriterInstanceAndWaitTaks());

    ApplicationUtil.runOnExecutorService(tasks);

    deleteClusterAndWait();

    deleteParameterGroups();

    log.info("MySQL cluster undeployment completed successfully");
  }

  private List<Callable<Void>> deleteReaderInstancesAndWaitTaks() {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (Application.getState().getReaderInstanceIdentifiers() != null
        && !Application.getState().getReaderInstanceIdentifiers().isEmpty()) {
      List<Map.Entry<String, String>> readerInstancesToDelete =
          new ArrayList<>(Application.getState().getReaderInstanceIdentifiers().entrySet());
      for (Map.Entry<String, String> entry : readerInstancesToDelete) {
        String readerInstanceIdentifier = entry.getValue();
        String key = entry.getKey();
        rdsClient.deleteDBInstance(
            readerInstanceIdentifier, Application.getState().getDeployConfig().getDeletionConfig());
        tasks.add(
            () -> {
              rdsClient.waitUntilDBInstanceDeleted(readerInstanceIdentifier);
              Application.getState().getReaderInstanceIdentifiers().remove(key);
              return null;
            });
      }
    }
    return tasks;
  }

  private List<Callable<Void>> deleteWriterInstanceAndWaitTaks() {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (Application.getState().getWriterInstanceIdentifier() != null) {
      rdsClient.deleteDBInstance(
          Application.getState().getWriterInstanceIdentifier(),
          Application.getState().getDeployConfig().getDeletionConfig());
      tasks.add(
          () -> {
            rdsClient.waitUntilDBInstanceDeleted(
                Application.getState().getWriterInstanceIdentifier());
            Application.getState().setWriterInstanceIdentifier(null);
            return null;
          });
    }
    return tasks;
  }

  private void deleteClusterAndWait() {
    if (Application.getState().getClusterIdentifier() != null) {
      rdsClient.deleteDBCluster(
          Application.getState().getClusterIdentifier(),
          Application.getState().getDeployConfig().getDeletionConfig());
      rdsClient.waitUntilDBClusterDeleted(Application.getState().getClusterIdentifier());
      Application.getState().setClusterIdentifier(null);
      Application.getState().setWriterEndpoint(null);
      Application.getState().setReaderEndpoint(null);
    }
  }

  private void deleteParameterGroups() {
    if (Application.getState().getClusterParameterGroupName() != null) {
      rdsClient.deleteDBClusterParameterGroup(
          Application.getState().getClusterParameterGroupName());
      Application.getState().setClusterParameterGroupName(null);
    }
    if (Application.getState().getWriterInstanceParameterGroupName() != null) {
      rdsClient.deleteDBParameterGroup(
          Application.getState().getWriterInstanceParameterGroupName());
      Application.getState().setWriterInstanceParameterGroupName(null);
    }

    if (Application.getState().getReaderInstanceParameterGroupNames() != null
        && !Application.getState().getReaderInstanceParameterGroupNames().isEmpty()) {
      List<Map.Entry<String, String>> readerParameterGroupsToDelete =
          new ArrayList<>(Application.getState().getReaderInstanceParameterGroupNames().entrySet());
      for (Map.Entry<String, String> entry : readerParameterGroupsToDelete) {
        String readerInstanceParameterGroupName = entry.getValue();
        String key = entry.getKey();
        rdsClient.deleteDBParameterGroup(readerInstanceParameterGroupName);
        Application.getState().getReaderInstanceParameterGroupNames().remove(key);
      }
    }
  }
}
