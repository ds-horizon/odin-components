package com.dream11.mysql.service;

import com.dream11.mysql.Application;
import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class UndeployService {
  private final RDSClient rdsClient;
  private final StateCorrectionService stateCorrectionService;

  public void undeployService() {
    this.stateCorrectionService.correctState();

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
    ApplicationUtil.runOnExecutorService(tasks);

    if (Application.getState().getClusterIdentifier() != null) {
      rdsClient.deleteDBCluster(
          Application.getState().getClusterIdentifier(),
          Application.getState().getDeployConfig().getDeletionConfig());
      rdsClient.waitUntilDBClusterDeleted(Application.getState().getClusterIdentifier());
      Application.getState().setClusterIdentifier(null);
      Application.getState().setWriterEndpoint(null);
      Application.getState().setReaderEndpoint(null);
    }

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
    log.info("MySQL cluster undeployment completed successfully");
  }
}
