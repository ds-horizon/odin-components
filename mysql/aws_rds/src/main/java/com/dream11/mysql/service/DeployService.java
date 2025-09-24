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
public class DeployService {
  private final DeployConfig deployConfig;
  private final ComponentMetadata componentMetadata;
  private final RDSClient rdsClient;
  private final StateCorrectionService stateCorrectionService;
  @NonNull final AwsAccountData awsAccountData;
  @NonNull final RDSData rdsData;

  public void deployService() {
    this.stateCorrectionService.correctState();
    List<Callable<Void>> tasks = new ArrayList<>();

    log.info("Starting with mysql deployment");
    String name =
        ApplicationUtil.joinByDash(
            componentMetadata.getComponentName(), componentMetadata.getEnvName());
    String randomId = ApplicationUtil.generateRandomId(4);

    Map<String, String> tags =
        ApplicationUtil.merge(List.of(this.awsAccountData.getTags(), this.deployConfig.getTags()));

    String clusterIdentifier = createClusterAndWait(name, randomId, tags);

    tasks.addAll(createWriterInstanceAndWaitTaks(name, randomId, clusterIdentifier, tags));
    tasks.addAll(createReaderInstancesAndWaitTaks(name, randomId, clusterIdentifier, tags));

    ApplicationUtil.runOnExecutorService(tasks);

    log.info("MySQL cluster deployment completed successfully");
  }

  private String createClusterAndWait(String name, String randomId, Map<String, String> tags) {
    String clusterParameterGroupName =
        deployConfig.getClusterParameterGroupName() != null
            ? deployConfig.getClusterParameterGroupName()
            : Application.getState().getClusterParameterGroupName();
    if (clusterParameterGroupName == null) {
      clusterParameterGroupName =
          ApplicationUtil.joinByDash(name, Constants.ClusterParameterGroupSuffix, randomId);
      rdsClient.createDBClusterParameterGroup(clusterParameterGroupName, tags, deployConfig);
      Application.getState().setClusterParameterGroupName(clusterParameterGroupName);
    }

    String clusterIdentifier = Application.getState().getClusterIdentifier();
    if (clusterIdentifier == null) {
      clusterIdentifier = ApplicationUtil.joinByDash(name, Constants.ClusterSuffix, randomId);
      List<String> endpoints =
          rdsClient.createDBCluster(
              clusterIdentifier, clusterParameterGroupName, tags, deployConfig, rdsData);
      Application.getState().setClusterIdentifier(clusterIdentifier);
      Application.getState().setWriterEndpoint(endpoints.get(0));
      Application.getState().setReaderEndpoint(endpoints.get(1));
      rdsClient.waitUntilDBClusterAvailable(clusterIdentifier);
    }
    return clusterIdentifier;
  }

  private List<Callable<Void>> createWriterInstanceAndWaitTaks(
      String name, String randomId, String clusterIdentifier, Map<String, String> tags) {
    List<Callable<Void>> tasks = new ArrayList<>();
    String writerInstanceParameterGroupName =
        deployConfig.getWriter().getInstanceParameterGroupName() != null
            ? deployConfig.getWriter().getInstanceParameterGroupName()
            : Application.getState().getWriterInstanceParameterGroupName();
    if (writerInstanceParameterGroupName == null) {
      writerInstanceParameterGroupName =
          ApplicationUtil.joinByDash(name, Constants.WriterInstanceParameterGroupSuffix, randomId);
      rdsClient.createDBInstanceParameterGroup(
          writerInstanceParameterGroupName,
          deployConfig.getVersion(),
          tags,
          deployConfig.getWriter().getInstanceParameterGroupConfig());
      Application.getState().setWriterInstanceParameterGroupName(writerInstanceParameterGroupName);
    }

    if (Application.getState().getWriterInstanceIdentifier() == null) {
      String writerInstanceIdentifier =
          ApplicationUtil.joinByDash(name, Constants.WriterInstanceSuffix, randomId);
      rdsClient.createDBInstance(
          writerInstanceIdentifier,
          clusterIdentifier,
          writerInstanceParameterGroupName,
          tags,
          deployConfig.getWriter());
      Application.getState().setWriterInstanceIdentifier(writerInstanceIdentifier);
      tasks.add(
          () -> {
            rdsClient.waitUntilDBInstanceAvailable(writerInstanceIdentifier);
            return null;
          });
    }
    return tasks;
  }

  private List<Callable<Void>> createReaderInstancesAndWaitTaks(
      String name, String randomId, String clusterIdentifier, Map<String, String> tags) {
    List<Callable<Void>> tasks = new ArrayList<>();
    if (deployConfig.getReaders() != null && !deployConfig.getReaders().isEmpty()) {
      if (Application.getState().getReaderInstanceIdentifiers() == null) {
        Application.getState().setReaderInstanceIdentifiers(new HashMap<>());
      }
      if (Application.getState().getReaderInstanceParameterGroupNames() == null) {
        Application.getState().setReaderInstanceParameterGroupNames(new HashMap<>());
      }

      for (int i = 0; i < deployConfig.getReaders().size(); i++) {
        String parameterGroupKey = String.valueOf(i);
        String instanceParameterGroupName =
            deployConfig.getReaders().get(i).getInstanceParameterGroupName() != null
                ? deployConfig.getReaders().get(i).getInstanceParameterGroupName()
                : Application.getState()
                    .getReaderInstanceParameterGroupNames()
                    .get(parameterGroupKey);
        if (instanceParameterGroupName == null) {
          instanceParameterGroupName =
              ApplicationUtil.joinByDash(
                  name, Constants.ReaderInstanceParameterGroupSuffix, parameterGroupKey, randomId);
          rdsClient.createDBInstanceParameterGroup(
              instanceParameterGroupName,
              deployConfig.getVersion(),
              tags,
              deployConfig.getReaders().get(i).getInstanceParameterGroupConfig());
          Application.getState()
              .getReaderInstanceParameterGroupNames()
              .put(parameterGroupKey, instanceParameterGroupName);
        }
        for (int j = 0; j < deployConfig.getReaders().get(i).getInstanceCount(); j++) {
          String instanceKey = ApplicationUtil.joinByDash(String.valueOf(i), String.valueOf(j));

          if (Application.getState().getReaderInstanceIdentifiers().get(instanceKey) == null) {
            String readerInstanceIdentifier =
                ApplicationUtil.joinByDash(
                    name, Constants.ReaderInstanceSuffix, instanceKey, randomId);
            rdsClient.createDBInstance(
                readerInstanceIdentifier,
                clusterIdentifier,
                instanceParameterGroupName,
                tags,
                deployConfig.getReaders().get(i));
            Application.getState()
                .getReaderInstanceIdentifiers()
                .put(instanceKey, readerInstanceIdentifier);

            tasks.add(
                () -> {
                  rdsClient.waitUntilDBInstanceAvailable(readerInstanceIdentifier);
                  return null;
                });
          }
        }
      }
    }

    return tasks;
  }
}
