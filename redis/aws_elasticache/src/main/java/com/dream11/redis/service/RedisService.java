package com.dream11.redis.service;

import java.util.List;
import java.util.Map;

import com.dream11.redis.Application;
import com.dream11.redis.client.RedisClient;
import com.dream11.redis.config.metadata.ComponentMetadata;
import com.dream11.redis.config.metadata.aws.AwsAccountData;
import com.dream11.redis.config.metadata.aws.RedisData;
import com.dream11.redis.config.user.DeployConfig;
import com.dream11.redis.constant.Constants;
import com.dream11.redis.util.ApplicationUtil;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroup;

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
      this.redisClient.waitUntilReplicationGroupAvailable(
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
    redisClient.waitForReplicationGroupDeletion(replicationGroupIdentifier);
    log.info(
        "Redis undeployment completed successfully for replicationGroup {}",
        replicationGroupIdentifier);
  }
}
