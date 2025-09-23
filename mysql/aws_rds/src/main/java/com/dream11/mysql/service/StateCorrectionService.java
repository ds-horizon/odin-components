package com.dream11.mysql.service;

import com.dream11.mysql.Application;
import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.exception.GenericApplicationException;
import com.dream11.mysql.state.State;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StateCorrectionService {

  @NonNull final RDSClient rdsClient;

  public void correctState() {
    State state = Application.getState();

    if (state.getClusterIdentifier() != null) {
      try {
        this.rdsClient.describeDBCluster(state.getClusterIdentifier());
      } catch (GenericApplicationException ex) {
        log.warn(
            "DB cluster:[{}] from state does not exist. Updating state.",
            state.getClusterIdentifier());
        state.setClusterIdentifier(null);
      }
    }

    if (state.getWriterInstanceIdentifier() != null) {
      try {
        this.rdsClient.describeDBInstance(state.getWriterInstanceIdentifier());
      } catch (GenericApplicationException ex) {
        log.warn(
            "DB instance:[{}] from state does not exist. Updating state.",
            state.getWriterInstanceIdentifier());
        state.setWriterInstanceIdentifier(null);
      }
    }

    if (state.getReaderInstanceIdentifiers() != null) {
      List<Map.Entry<String, String>> readerInstanceIdentifiers =
          new ArrayList<>(state.getReaderInstanceIdentifiers().entrySet());
      for (Map.Entry<String, String> entry : readerInstanceIdentifiers) {
        try {
          this.rdsClient.describeDBInstance(entry.getValue());
        } catch (GenericApplicationException ex) {
          log.warn("DB instance:[{}] from state does not exist. Updating state.", entry.getValue());
          readerInstanceIdentifiers.remove(entry);
        }
      }
    }

    if (state.getClusterParameterGroupName() != null) {
      try {
        this.rdsClient.describeDBClusterParameterGroup(state.getClusterParameterGroupName());
      } catch (GenericApplicationException ex) {
        log.warn(
            "DB cluster parameter group:[{}] from state does not exist. Updating state.",
            state.getClusterParameterGroupName());
        state.setClusterParameterGroupName(null);
      }
    }

    if (state.getWriterInstanceParameterGroupName() != null) {
      try {
        this.rdsClient.describeDBParameterGroup(state.getWriterInstanceParameterGroupName());
      } catch (GenericApplicationException ex) {
        log.warn(
            "DB parameter group:[{}] from state does not exist. Updating state.",
            state.getWriterInstanceParameterGroupName());
        state.setWriterInstanceParameterGroupName(null);
      }
    }

    if (state.getReaderInstanceParameterGroupNames() != null) {
      List<Map.Entry<String, String>> readerInstanceParameterGroupNames =
          new ArrayList<>(state.getReaderInstanceParameterGroupNames().entrySet());
      for (Map.Entry<String, String> entry : readerInstanceParameterGroupNames) {
        try {
          this.rdsClient.describeDBParameterGroup(entry.getValue());
        } catch (GenericApplicationException ex) {
          log.warn(
              "DB parameter group:[{}] from state does not exist. Updating state.",
              entry.getValue());
          readerInstanceParameterGroupNames.remove(entry);
        }
      }
    }
  }
}
