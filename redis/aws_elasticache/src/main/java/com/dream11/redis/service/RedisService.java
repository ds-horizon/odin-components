package com.dream11.redis.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.dream11.redis.Application;
import com.dream11.redis.client.RedisClient;
import com.dream11.redis.config.metadata.ComponentMetadata;
import com.dream11.redis.config.metadata.aws.AwsAccountData;
import com.dream11.redis.config.metadata.aws.RedisData;
import com.dream11.redis.config.user.DeployConfig;
import com.dream11.redis.config.user.UpdateNodeGroupCountConfig;
import com.dream11.redis.config.user.UpdateNodeTypeConfig;
import com.dream11.redis.config.user.UpdateReplicaCountConfig;
import com.dream11.redis.constant.Constants;
import com.dream11.redis.error.ApplicationError;
import com.dream11.redis.exception.GenericApplicationException;
import com.dream11.redis.state.State;
import com.dream11.redis.util.ApplicationUtil;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.elasticache.model.NodeGroup;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroup;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroupNotFoundException;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class RedisService {
  @NonNull
  final DeployConfig deployConfig;
  @NonNull
  final ComponentMetadata componentMetadata;
  @NonNull
  final RedisClient redisClient;
  @NonNull
  final AwsAccountData awsAccountData;
  @NonNull
  final RedisData redisData;

  public void reconcileState() {
    State state = Application.getState();

    if (state.getReplicationGroupIdentifier() != null) {
      try {
        redisClient.getReplicationGroup(state.getReplicationGroupIdentifier());
        log.debug("Found replication group: {}", state.getReplicationGroupIdentifier());
      } catch (ReplicationGroupNotFoundException ex) {
        log.warn(
            "Redis replicationGroup :[{}] from state does not exist. Updating state.",
            state.getReplicationGroupIdentifier());
        state.setReplicationGroupIdentifier(null);
      }
    }
  }

  public void deploy() {

    String identifier = Application.getState().getIdentifier();
    if (identifier == null) {
      identifier = ApplicationUtil.generateRandomId(4);
      Application.getState().setIdentifier(identifier);
    }
    String name = String.join(
        "-", this.componentMetadata.getComponentName(), this.componentMetadata.getEnvName());

    Map<String, String> tags = ApplicationUtil.merge(
        List.of(
            this.deployConfig.getTags(),
            this.awsAccountData.getTags(),
            Constants.COMPONENT_TAGS));

    createReplicationGroupAndWait(name, identifier, tags);
    log.info("Redis cluster deployment completed successfully");
  }

  private void createReplicationGroupAndWait(
      String name, String identifier, Map<String, String> tags) {
    String replicationGroupIdentifier = Application.getState().getReplicationGroupIdentifier();
    if (replicationGroupIdentifier == null) {
      replicationGroupIdentifier = String.join("-", name, identifier);

      log.info("Creating Replication group: {}", replicationGroupIdentifier);

      this.redisClient.createReplicationGroup(
          replicationGroupIdentifier, tags, this.deployConfig, this.redisData);

      log.info("Waiting for Replication group to become available: {}", replicationGroupIdentifier);
      waitUntilReplicationGroupAvailable(
          replicationGroupIdentifier,
          Constants.REPLICATION_GROUP_WAIT_RETRY_TIMEOUT,
          Constants.REPLICATION_GROUP_WAIT_RETRY_INTERVAL);
      log.info("Replication group is now available: {}", replicationGroupIdentifier);
      ReplicationGroup replicationGroup = redisClient.getReplicationGroup(replicationGroupIdentifier);

      Application.getState().setReplicationGroupIdentifier(replicationGroupIdentifier);
      if (deployConfig.getNumNodeGroups() > 1 || deployConfig.getClusterModeEnabled()) {
        Application.getState()
            .setPrimaryEndpoint(replicationGroup.configurationEndpoint().address());
        Application.getState()
            .setReaderEndpoint(replicationGroup.configurationEndpoint().address());
      } else {
        Application.getState()
            .setPrimaryEndpoint(replicationGroup.nodeGroups().get(0).primaryEndpoint().address());
        Application.getState()
            .setReaderEndpoint(replicationGroup.nodeGroups().get(0).readerEndpoint().address());
      }
    }
  }

  public void undeploy() {
    String replicationGroupIdentifier = Application.getState().getReplicationGroupIdentifier();
    log.info("Undeploying Redis replication group: {}", replicationGroupIdentifier);
    redisClient.deleteReplicationGroup(replicationGroupIdentifier);
    waitForReplicationGroupDeletion(replicationGroupIdentifier);
    log.info(
        "Redis undeployment completed successfully for replicationGroup {}",
        replicationGroupIdentifier);
  }

  @SneakyThrows
  public void waitUntilReplicationGroupAvailable(
      String replicationGroupId, Duration timeout, Duration interval) {
    long end = System.currentTimeMillis() + timeout.toMillis();
    while (System.currentTimeMillis() < end) {
      String status = redisClient.getReplicationGroupStatus(replicationGroupId);
      log.info("replicationGroupId: {} current status: {}", replicationGroupId, status);
      if ("available".equalsIgnoreCase(redisClient.getReplicationGroupStatus(replicationGroupId)))
        return;
      Thread.sleep(interval.toMillis());
    }
    throw new GenericApplicationException(ApplicationError.REPLICATION_GROUP_WAIT_TIMEOUT, replicationGroupId,
        "available");
  }

  @SneakyThrows
  public void waitForReplicationGroupDeletion(String replicationGroupId) {
    log.info("Waiting for replication group {} to be deleted...", replicationGroupId);

    long endTime = System.currentTimeMillis() + Constants.REPLICATION_GROUP_WAIT_RETRY_TIMEOUT.toMillis();

    while (System.currentTimeMillis() < endTime) {
      try {

        String status = redisClient.getReplicationGroupStatus(replicationGroupId);
        log.info("Replication group {} deletion in progress. Current status: {}", replicationGroupId, status);

        Thread.sleep(Constants.REPLICATION_GROUP_WAIT_RETRY_INTERVAL.toMillis());

      } catch (software.amazon.awssdk.services.elasticache.model.ReplicationGroupNotFoundException e) {
        // Replication group not found means it's been successfully deleted
        log.info("Replication group {} has been successfully deleted", replicationGroupId);
        return;
      }
    }

    throw new GenericApplicationException(
        ApplicationError.REPLICATION_GROUP_WAIT_TIMEOUT,
        replicationGroupId,
        "deleted");

  }

  public void updateNodeType(@NonNull UpdateNodeTypeConfig updateNodeTypeConfig) {
    String replicationGroupId = Application.getState().getReplicationGroupIdentifier();
    log.info(
        "Updating node type of replication group: {} to {}",
        replicationGroupId,
        updateNodeTypeConfig);
    String currentCacheNodeType = this.redisClient.getCacheNodeType(replicationGroupId);
    String desiredCacheNodeType = updateNodeTypeConfig.getCacheNodeType();
    if (currentCacheNodeType.equals(desiredCacheNodeType)) {
      log.info("Cache node type is already {}, no update needed", currentCacheNodeType);
      return;
    }
    this.redisClient.updateCacheNodeType(replicationGroupId, desiredCacheNodeType);

    log.info("Waiting for Replication group modification to complete: {}");
    waitUntilReplicationGroupAvailable(
        replicationGroupId,
        Constants.REPLICATION_GROUP_WAIT_RETRY_TIMEOUT,
        Constants.REPLICATION_GROUP_WAIT_RETRY_INTERVAL);
    log.info("Node type update completed successfully for replication group: {}", replicationGroupId);
  }

  public void updateReplicaCount(@NonNull UpdateReplicaCountConfig updateReplicaCountConfig) {
    String replicationGroupId = Application.getState().getReplicationGroupIdentifier();

    log.info("Updating replica count for replication group: {}", replicationGroupId);
    ReplicationGroup replicationGroup = redisClient.getReplicationGroup(replicationGroupId);
    log.debug("Replication group found: {}, Status: {}, ClusterEnabled: {}, AutomaticFailover: {}",
        replicationGroup.replicationGroupId(),
        replicationGroup.status(),
        replicationGroup.clusterEnabled(),
        replicationGroup.automaticFailover());

    if (Boolean.TRUE.equals(replicationGroup.clusterEnabled())) {
      String automaticFailoverStatus = replicationGroup.automaticFailover() != null
          ? replicationGroup.automaticFailover().toString()
          : null;
      if (automaticFailoverStatus == null || !"enabled".equalsIgnoreCase(automaticFailoverStatus)) {
        throw new GenericApplicationException(
            ApplicationError.CONSTRAINT_VIOLATION,
            String.format(
                "Automatic failover must be enabled to change replica count for replication group %s. "
                    + "Current automatic failover status: %s. ",
                replicationGroupId, automaticFailoverStatus));
      }

    }

    List<NodeGroup> nodeGroups = replicationGroup.nodeGroups();
    int current = nodeGroups.get(0).nodeGroupMembers().size() - 1;
    if (current == updateReplicaCountConfig.getReplicasPerNodeGroup()) {
      log.info("No change: replicas per shard already {}", current);
      return;
    }
    if (updateReplicaCountConfig.getReplicasPerNodeGroup() > current) {
      log.info("Increasing replica count to {} for replication group id: {}",
          replicationGroupId, updateReplicaCountConfig.getReplicasPerNodeGroup());
      redisClient.increaseReplicaCount(replicationGroupId, updateReplicaCountConfig.getReplicasPerNodeGroup());
    } else {
      log.info("Decreasing replica count to {} for replication group id: {}",
          replicationGroupId, updateReplicaCountConfig.getReplicasPerNodeGroup());
      redisClient.decreaseReplicaCount(replicationGroupId, updateReplicaCountConfig.getReplicasPerNodeGroup());
    }
    log.info("Waiting for Replication group to become available: {}", replicationGroupId);
    waitUntilReplicationGroupAvailable(replicationGroupId,
        Constants.REPLICATION_GROUP_WAIT_RETRY_TIMEOUT, Constants.REPLICATION_GROUP_WAIT_RETRY_INTERVAL);
    log.info("Replication group is now available: {}", replicationGroupId);

  }

  public void updateNodeGroupCount(@NonNull UpdateNodeGroupCountConfig updateNodeGroupCountConfig) {
    String replicationGroupId = Application.getState().getReplicationGroupIdentifier();
    log.info("Updating node group count for replication group: {}", replicationGroupId);
    ReplicationGroup replicationGroup = redisClient.getReplicationGroup(replicationGroupId);
    log.debug("Replication group found: {}, Status: {}, ClusterEnabled: {}, AutomaticFailover: {}",
        replicationGroup.replicationGroupId(),
        replicationGroup.status(),
        replicationGroup.clusterEnabled(),
        replicationGroup.automaticFailover());

    if (Boolean.FALSE.equals(replicationGroup.clusterEnabled())) {
      throw new GenericApplicationException(
          ApplicationError.CONSTRAINT_VIOLATION,
          String.format(
              "Changing node group count is only supported for Cluster Mode Enabled (CME) replication groups. "
                  + "Replication group %s is in Cluster Mode Disabled (CMD) mode.",
              replicationGroupId));
    }
    List<NodeGroup> nodeGroups = replicationGroup.nodeGroups();
    if (nodeGroups.size() == updateNodeGroupCountConfig.getNumNodeGroups()) {
      log.info("No change: node group count already {}", nodeGroups.size());
      return;
    }

    redisClient.updateReplicationGroupShardConfiguration(replicationGroupId,
        updateNodeGroupCountConfig.getNumNodeGroups());
    log.info("Node group count updated successfully for replication group: {}", replicationGroupId);
    log.info("Waiting for Replication group to become available: {}", replicationGroupId);
    waitUntilReplicationGroupAvailable(
        replicationGroupId,
        Constants.REPLICATION_GROUP_WAIT_RETRY_TIMEOUT,
        Constants.REPLICATION_GROUP_WAIT_RETRY_INTERVAL);
    log.info("Replication group is now available: {}", replicationGroupId);
  }
}
