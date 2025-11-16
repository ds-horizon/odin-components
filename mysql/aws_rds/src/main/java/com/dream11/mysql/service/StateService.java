package com.dream11.mysql.service;

import com.dream11.mysql.Application;
import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.config.user.ReaderConfig;
import com.dream11.mysql.config.user.WriterConfig;
import com.dream11.mysql.exception.DBClusterNotFoundException;
import com.dream11.mysql.exception.DBClusterParameterGroupNotFoundException;
import com.dream11.mysql.exception.DBParameterGroupNotFoundException;
import com.dream11.mysql.state.State;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DBClusterMember;
import software.amazon.awssdk.services.rds.model.DBInstance;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StateService {

  @NonNull final RDSClient rdsClient;

  public void reconcileState() {
    State state = Application.getState();
    log.debug("Reconciling state...");

    if (state.getClusterParameterGroupName() != null) {
      try {
        this.rdsClient.getDBClusterParameterGroup(state.getClusterParameterGroupName());
        log.debug("Found cluster parameter group: {}", state.getClusterParameterGroupName());
      } catch (DBClusterParameterGroupNotFoundException ex) {
        log.warn(
            "DB cluster parameter group:[{}] from state does not exist. Updating state.",
            state.getClusterParameterGroupName());
        state.setClusterParameterGroupName(null);
      }
    }

    if (state.getInstanceParameterGroupName() != null) {
      try {
        this.rdsClient.getDBParameterGroup(state.getInstanceParameterGroupName());
        log.debug("Found instance parameter group: {}", state.getInstanceParameterGroupName());
      } catch (DBParameterGroupNotFoundException ex) {
        log.warn(
            "DB instance parameter group:[{}] from state does not exist. Updating state.",
            state.getInstanceParameterGroupName());
        state.setInstanceParameterGroupName(null);
      }
    }

    if (state.getClusterIdentifier() != null) {
      try {
        DBCluster cluster = this.rdsClient.getDBCluster(state.getClusterIdentifier());
        populateStateFromCluster(cluster, state);
        log.debug("Found cluster: {}", state.getClusterIdentifier());
      } catch (DBClusterNotFoundException ex) {
        log.warn(
            "DB cluster:[{}] from state does not exist. Updating state.",
            state.getClusterIdentifier());
        state.setClusterIdentifier(null);
      }
    }
  }

  private void populateStateFromCluster(DBCluster cluster, State state) {
    state.setWriterInstanceIdentifier(null);
    state.getReaderInstanceIdentifiers().clear();
    Map<String, ReaderConfig> readers = new HashMap<>();

    for (DBClusterMember member : cluster.dbClusterMembers()) {
      String instanceIdentifier = member.dbInstanceIdentifier();
      DBInstance instance = this.rdsClient.getDBInstance(instanceIdentifier);
      String instanceType = instance.dbInstanceClass();
      Integer promotionTier = instance.promotionTier();

      if (member.isClusterWriter().equals(Boolean.TRUE)) {
        state.setWriterInstanceIdentifier(instanceIdentifier);
        if (state.getDeployConfig() != null) {
          state
              .getDeployConfig()
              .setWriter(
                  WriterConfig.builder()
                      .instanceType(instanceType)
                      .promotionTier(promotionTier)
                      .build());
        }
        log.debug("Found writer instance: {}", instanceIdentifier);
      } else {
        state
            .getReaderInstanceIdentifiers()
            .computeIfAbsent(instanceType, k -> new ArrayList<>())
            .add(instanceIdentifier);
        readers
            .computeIfAbsent(
                instanceType,
                type ->
                    ReaderConfig.builder()
                        .instanceCount(0)
                        .instanceType(type)
                        .promotionTier(promotionTier)
                        .build())
            .setInstanceCount(readers.get(instanceType).getInstanceCount() + 1);
        log.debug("Found reader instance: {} of type: {}", instanceIdentifier, instanceType);
      }
    }
    if (state.getDeployConfig() != null) {
      state.getDeployConfig().setReaders(new ArrayList<>(readers.values()));
    }
  }
}
