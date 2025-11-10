package com.dream11.redis.client;

import com.dream11.redis.config.metadata.aws.RedisData;
import com.dream11.redis.config.user.DeployConfig;
import com.dream11.redis.config.user.UpdateReplicaCountConfig;
import com.dream11.redis.constant.Constants;
import com.dream11.redis.error.ApplicationError;
import com.dream11.redis.exception.GenericApplicationException;
import com.dream11.redis.exception.ReplicationGroupNotFoundException;
import com.dream11.redis.util.ApplicationUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CreateReplicationGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DecreaseReplicaCountRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteReplicationGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeReplicationGroupsRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeReplicationGroupsResponse;
import software.amazon.awssdk.services.elasticache.model.IncreaseReplicaCountRequest;
import software.amazon.awssdk.services.elasticache.model.NodeGroup;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroup;
import software.amazon.awssdk.services.elasticache.model.Tag;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RedisClient {
  final ElastiCacheClient elastiCacheClient;

  public RedisClient(String region) {
    this.elastiCacheClient =
        ElastiCacheClient.builder()
            .region(Region.of(region))
            .overrideConfiguration(
                overrideConfig ->
                    overrideConfig
                        .retryStrategy(
                            AwsRetryStrategy.standardRetryStrategy().toBuilder()
                                .maxAttempts(Constants.AWS_CLIENT_MAX_ATTEMPTS_SECONDS)
                                .throttlingBackoffStrategy(
                                    BackoffStrategy.exponentialDelayHalfJitter(
                                        Duration.ofSeconds(
                                            Constants.AWS_CLIENT_RETRY_DELAY_SECONDS),
                                        Duration.ofSeconds(
                                            Constants.AWS_CLIENT_RETRY_MAX_BACKOFF_SECONDS)))
                                .build())
                        .apiCallTimeout(Duration.ofMinutes(2))
                        .apiCallAttemptTimeout(Duration.ofSeconds(30)))
            .build();
  }

  /**
   * Creates an ElastiCache Redis replication group from scratch.
   *
   * @param replicationGroupId The identifier for the replication group
   * @param tags Tags to apply to the replication group
   * @param deployConfig Configuration for deployment
   * @param redisData Metadata containing subnet groups and security groups
   * @return The primary endpoint of the replication group
   */
  public void createReplicationGroup(
      String replicationGroupId,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RedisData redisData) {

    if (deployConfig.getCacheParameterGroupName() == null) {
      // Input ensures that the redisVersion is mandatory and all valid values have at least one
      // character
      String cacheParameterGroupName =
          String.join(
              ".",
              Constants.DEFAULT,
              Constants.ENGINE_TYPE + deployConfig.getRedisVersion().charAt(0));
      if (deployConfig.getNumNodeGroups() > 1) {
        cacheParameterGroupName += Constants.PARAMETER_GROUP_SUFFIX;
      }
      deployConfig.setCacheParameterGroupName(cacheParameterGroupName);
    }

    CreateReplicationGroupRequest.Builder createBuilder = CreateReplicationGroupRequest.builder();

    applyCommonConfiguration(createBuilder, replicationGroupId, tags, deployConfig, redisData);

    elastiCacheClient.createReplicationGroup(createBuilder.build());
  }

  /** Helper method to apply common configuration to the replication group builder. */
  private void applyCommonConfiguration(
      CreateReplicationGroupRequest.Builder builder,
      String replicationGroupId,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RedisData redisData) {

    // Set required fields
    builder.replicationGroupId(replicationGroupId);
    builder.replicationGroupDescription(deployConfig.getReplicationGroupDescription());
    builder.engine(Constants.ENGINE_TYPE);
    builder.cacheNodeType(deployConfig.getCacheNodeType());
    builder.engineVersion(deployConfig.getRedisVersion());

    // Set parameter group if provided
    ApplicationUtil.setIfNotNull(
        builder::cacheParameterGroupName, deployConfig.getCacheParameterGroupName());

    if (deployConfig.getCacheSubnetGroupName() != null) {
      builder.cacheSubnetGroupName(deployConfig.getCacheSubnetGroupName());
    } else if (redisData.getSubnetGroup() != null) {
      builder.cacheSubnetGroupName(redisData.getSubnetGroup());
    } else {
      throw new GenericApplicationException(ApplicationError.SUBNET_GROUP_NOT_FOUND);
    }

    if (deployConfig.getSecurityGroupIds() != null
        && !deployConfig.getSecurityGroupIds().isEmpty()) {
      builder.securityGroupIds(deployConfig.getSecurityGroupIds());
    } else if (redisData.getSecurityGroups() != null && !redisData.getSecurityGroups().isEmpty()) {
      builder.securityGroupIds(redisData.getSecurityGroups());
    } else {
      throw new GenericApplicationException(ApplicationError.SECURITY_GROUP_NOT_FOUND);
    }

    // Set tags
    builder.tags(convertMapToTags(tags));

    // Set cluster configuration
    ApplicationUtil.setIfNotNull(builder::numNodeGroups, deployConfig.getNumNodeGroups());
    ApplicationUtil.setIfNotNull(
        builder::replicasPerNodeGroup, deployConfig.getReplicasPerNodeGroup());

    // Set optional boolean flags
    ApplicationUtil.setIfNotNull(
        builder::automaticFailoverEnabled, deployConfig.getAutomaticFailoverEnabled());
    ApplicationUtil.setIfNotNull(builder::multiAZEnabled, deployConfig.getMultiAzEnabled());
    ApplicationUtil.setIfNotNull(
        builder::transitEncryptionEnabled, deployConfig.getTransitEncryptionEnabled());
    ApplicationUtil.setIfNotNull(
        builder::atRestEncryptionEnabled, deployConfig.getAtRestEncryptionEnabled());

    // Set snapshot and backup configuration
    ApplicationUtil.setIfNotNull(
        builder::snapshotRetentionLimit, deployConfig.getSnapshotRetentionLimit());
    ApplicationUtil.setIfNotNull(builder::snapshotWindow, deployConfig.getSnapshotWindow());

    // Set maintenance window
    ApplicationUtil.setIfNotNull(
        builder::preferredMaintenanceWindow, deployConfig.getPreferredMaintenanceWindow());

    // Set notification topic
    ApplicationUtil.setIfNotNull(
        builder::notificationTopicArn, deployConfig.getNotificationTopicArn());

    // Set auto minor version upgrade
    ApplicationUtil.setIfNotNull(
        builder::autoMinorVersionUpgrade, deployConfig.getAutoMinorVersionUpgrade());

    ApplicationUtil.setIfNotNull(
        builder::preferredCacheClusterAZs, deployConfig.getPreferredCacheClusterAZs());

    ApplicationUtil.setIfNotNull(builder::kmsKeyId, deployConfig.getKmsKeyId());
  }

  @SneakyThrows
  public void waitUntilReplicationGroupAvailable(
      String replicationGroupId, Duration timeout, Duration interval) {
    long end = System.currentTimeMillis() + timeout.toMillis();
    while (true) {
      ReplicationGroup rg =
          elastiCacheClient
              .describeReplicationGroups(b -> b.replicationGroupId(replicationGroupId))
              .replicationGroups()
              .get(0);
      if ("available".equalsIgnoreCase(rg.status())) return;
      if (System.currentTimeMillis() > end)
        throw new GenericApplicationException(
            ApplicationError.REPLICATION_GROUP_WAIT_TIMEOUT, replicationGroupId, "available");
      Thread.sleep(interval.toMillis());
    }
  }

  /** Converts a map of tags to ElastiCache Tag objects. */
  private List<Tag> convertMapToTags(Map<String, String> tagMap) {
    return tagMap.entrySet().stream()
        .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
        .collect(Collectors.toList());
  }

  /**
   * Retrieves a replication group by its identifier.
   *
   * @param replicationGroupId The identifier of the replication group
   * @return The ReplicationGroup object
   * @throws ReplicationGroupNotFoundException if the replication group is not found
   */
  public ReplicationGroup getReplicationGroup(String replicationGroupId) {
    DescribeReplicationGroupsRequest request =
        DescribeReplicationGroupsRequest.builder().replicationGroupId(replicationGroupId).build();

    return this.elastiCacheClient.describeReplicationGroups(request).replicationGroups().stream()
        .findFirst()
        .orElseThrow(() -> new ReplicationGroupNotFoundException(replicationGroupId));
  }

  @SneakyThrows
  public void deleteReplicationGroup(String replicationGroupId) {

    elastiCacheClient.deleteReplicationGroup(
        DeleteReplicationGroupRequest.builder().replicationGroupId(replicationGroupId).build());

    while (true) {
      try {
        DescribeReplicationGroupsResponse replicationGroupResponse =
            elastiCacheClient.describeReplicationGroups(
                DescribeReplicationGroupsRequest.builder()
                    .replicationGroupId(replicationGroupId)
                    .build());
        // Given the groupId is passed in the request, this will always have replicationGroups or
        // throw the ReplicationGroupNotFoundException
        String status = replicationGroupResponse.replicationGroups().get(0).status();
        log.debug("Current deletion status: {}", status);
        Thread.sleep(Constants.REPLICATION_GROUP_WAIT_RETRY_INTERVAL.toMillis());
      } catch (
          software.amazon.awssdk.services.elasticache.model.ReplicationGroupNotFoundException e) {
        log.debug(
            "Replication group {} not found exception, it has been deleted now",
            replicationGroupId);
        break;
      }
    }
  }

    public void updateReplicaCount(String replicationGroupIdentifier, @NonNull UpdateReplicaCountConfig updateReplicaCountConfig) {

      ReplicationGroup replicationGroup = elastiCacheClient.describeReplicationGroups(
              DescribeReplicationGroupsRequest.builder().replicationGroupId(replicationGroupIdentifier).build()
      ).replicationGroups().get(0);


      // For CMD there is a single node group
      List<NodeGroup> nodeGroups = replicationGroup.nodeGroups();

      // Compute current replicas-per-shard (assume uniform; if not, we’ll still set uniformly)
      int current = nodeGroups.get(0).nodeGroupMembers().size() - 1;

      if (updateReplicaCountConfig.getReplicasPerNodeGroup() == current) {
        log.info("No change: replicas per shard already {}", current);
        return;
      }

      if (updateReplicaCountConfig.getReplicasPerNodeGroup() > current) {
        elastiCacheClient.increaseReplicaCount(IncreaseReplicaCountRequest.builder()
                .replicationGroupId(replicationGroupIdentifier)
                .newReplicaCount(updateReplicaCountConfig.getReplicasPerNodeGroup())
                .applyImmediately(true).build());

      } else {

        elastiCacheClient.decreaseReplicaCount(DecreaseReplicaCountRequest.builder()
                .replicationGroupId(replicationGroupIdentifier)
                .newReplicaCount(updateReplicaCountConfig.getReplicasPerNodeGroup())
                .applyImmediately(true).build());

      }
    }




    }
