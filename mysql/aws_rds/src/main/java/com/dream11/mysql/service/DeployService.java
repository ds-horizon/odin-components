package com.dream11.mysql.service;

import com.dream11.mysql.Application;
import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.config.metadata.ComponentMetadata;
import com.dream11.mysql.config.metadata.aws.AwsAccountData;
import com.dream11.mysql.config.metadata.aws.RDSData;
import com.dream11.mysql.config.user.DeployConfig;
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
    String name = componentMetadata.getComponentName() + "-" + componentMetadata.getEnvName();

    Map<String, String> tags =
        ApplicationUtil.merge(List.of(this.awsAccountData.getTags(), this.deployConfig.getTags()));

    String clusterParameterGroupName =
        deployConfig.getClusterParameterGroupName() != null
            ? deployConfig.getClusterParameterGroupName()
            : Application.getState().getClusterParameterGroupName();
    if (clusterParameterGroupName == null) {
      clusterParameterGroupName = name + "-cpg-" + ApplicationUtil.generateRandomId(4);
      rdsClient.createClusterParameterGroup(clusterParameterGroupName, tags, deployConfig);
      Application.getState().setClusterParameterGroupName(clusterParameterGroupName);
    }

    String clusterIdentifier = Application.getState().getClusterIdentifier();
    if (clusterIdentifier == null) {
      clusterIdentifier = name + "-cluster-" + ApplicationUtil.generateRandomId(4);
      List<String> endpoints =
          rdsClient.createDBCluster(
              clusterIdentifier, clusterParameterGroupName, tags, deployConfig, rdsData);
      Application.getState().setClusterIdentifier(clusterIdentifier);
      Application.getState().setWriterEndpoint(endpoints.get(0));
      Application.getState().setReaderEndpoint(endpoints.get(1));
      rdsClient.waitUntilDBClusterAvailable(clusterIdentifier);
    }

    String writerInstanceParameterGroupName =
        deployConfig.getWriter().getInstanceParameterGroupName() != null
            ? deployConfig.getWriter().getInstanceParameterGroupName()
            : Application.getState().getWriterInstanceParameterGroupName();
    if (writerInstanceParameterGroupName == null) {
      writerInstanceParameterGroupName = name + "-wpg-" + ApplicationUtil.generateRandomId(4);
      rdsClient.createInstanceParameterGroup(
          writerInstanceParameterGroupName,
          deployConfig.getVersion(),
          tags,
          deployConfig.getWriter().getInstanceParameterGroupConfig());
      Application.getState().setWriterInstanceParameterGroupName(writerInstanceParameterGroupName);
    }

    if (Application.getState().getWriterInstanceIdentifier() == null) {
      String writerInstanceIdentifier = name + "-writer-" + ApplicationUtil.generateRandomId(4);
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

    if (deployConfig.getReaders() != null && !deployConfig.getReaders().isEmpty()) {
      if (Application.getState().getReaderInstanceIdentifiers() == null) {
        Application.getState().setReaderInstanceIdentifiers(new HashMap<>());
      }
      if (Application.getState().getReaderInstanceParameterGroupNames() == null) {
        Application.getState().setReaderInstanceParameterGroupNames(new HashMap<>());
      }

      for (int i = 0; i < deployConfig.getReaders().size(); i++) {
        for (int j = 0; j < deployConfig.getReaders().get(i).getInstanceCount(); j++) {
          String key = i + "-" + j;
          String instanceParameterGroupName =
              deployConfig.getReaders().get(i).getInstanceParameterGroupName() != null
                  ? deployConfig.getReaders().get(i).getInstanceParameterGroupName()
                  : Application.getState().getReaderInstanceParameterGroupNames().get(key);
          if (instanceParameterGroupName == null) {
            instanceParameterGroupName = name + "-rpg-" + ApplicationUtil.generateRandomId(4);
            rdsClient.createInstanceParameterGroup(
                instanceParameterGroupName,
                deployConfig.getVersion(),
                tags,
                deployConfig.getReaders().get(i).getInstanceParameterGroupConfig());
            Application.getState()
                .getReaderInstanceParameterGroupNames()
                .put(key, instanceParameterGroupName);
          }
          if (Application.getState().getReaderInstanceIdentifiers().get(key) == null) {
            String readerInstanceIdentifier =
                name + "-reader-" + i + "-" + j + "-" + ApplicationUtil.generateRandomId(4);
            rdsClient.createDBInstance(
                readerInstanceIdentifier,
                clusterIdentifier,
                instanceParameterGroupName,
                tags,
                deployConfig.getReaders().get(i));
            Application.getState()
                .getReaderInstanceIdentifiers()
                .put(key, readerInstanceIdentifier);

            tasks.add(
                () -> {
                  rdsClient.waitUntilDBInstanceAvailable(readerInstanceIdentifier);
                  return null;
                });
          }
        }
      }
    }

    ApplicationUtil.runOnExecutorService(tasks);

    log.info("MySQL cluster deployment completed successfully");
  }
}
