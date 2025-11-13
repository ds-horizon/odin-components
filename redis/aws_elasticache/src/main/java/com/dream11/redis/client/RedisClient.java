package com.dream11.redis.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.dream11.redis.config.metadata.aws.RedisData;
import com.dream11.redis.config.user.CloudWatchLogsDetails;
import com.dream11.redis.config.user.DeployConfig;
import com.dream11.redis.config.user.DestinationDetails;
import com.dream11.redis.config.user.DestinationType;
import com.dream11.redis.config.user.KinesisFirehoseDetails;
import com.dream11.redis.config.user.LogDeliveryConfig;
import com.dream11.redis.constant.Constants;
import com.dream11.redis.error.ApplicationError;
import com.dream11.redis.exception.GenericApplicationException;
import com.dream11.redis.exception.ReplicationGroupNotFoundException;
import com.dream11.redis.util.ApplicationUtil;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CloudWatchLogsDestinationDetails;
import software.amazon.awssdk.services.elasticache.model.CreateReplicationGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteReplicationGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeReplicationGroupsRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeReplicationGroupsResponse;
import software.amazon.awssdk.services.elasticache.model.KinesisFirehoseDestinationDetails;
import software.amazon.awssdk.services.elasticache.model.LogDeliveryConfigurationRequest;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroup;
import software.amazon.awssdk.services.elasticache.model.Tag;

@Slf4j
public class RedisClient {
  final ElastiCacheClient elastiCacheClient;

  public RedisClient(String region) {
    elastiCacheClient = ElastiCacheClient.builder()
        .region(Region.of(region))
        .overrideConfiguration(
            overrideConfig -> overrideConfig
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
                .apiCallTimeout(Constants.AWS_CLIENT_API_CALL_TIMEOUT)
                .apiCallAttemptTimeout(Constants.AWS_CLIENT_API_CALL_ATTEMPT_TIMEOUT))
        .build();
  }

  /**
   * Creates an ElastiCache Redis replication group from scratch.
   *
   * @param replicationGroupId The identifier for the replication group
   * @param tags               Tags to apply to the replication group
   * @param deployConfig       Configuration for deployment
   * @param redisData          Metadata containing subnet groups and security
   *                           groups
   */
  public void createReplicationGroup(
      String replicationGroupId,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RedisData redisData) {

    String cacheParameterGroupName = deployConfig.getCacheParameterGroupName();
    if (cacheParameterGroupName == null) {
      cacheParameterGroupName = String.join(
          ".",
          Constants.DEFAULT,
          Constants.ENGINE_TYPE + deployConfig.getRedisVersion().charAt(0));
      if (deployConfig.getNumNodeGroups() > 1 || deployConfig.getClusterModeEnabled()) {
        cacheParameterGroupName += Constants.PARAMETER_GROUP_SUFFIX;
      }
    }

    CreateReplicationGroupRequest.Builder createBuilder = CreateReplicationGroupRequest.builder();

    applyCommonConfiguration(createBuilder, replicationGroupId, tags, deployConfig, redisData, cacheParameterGroupName);

    elastiCacheClient.createReplicationGroup(createBuilder.build());
  }

  /**
   * Helper method to apply common configuration to the replication group builder.
   */
  private void applyCommonConfiguration(
      CreateReplicationGroupRequest.Builder builder,
      String replicationGroupId,
      Map<String, String> tags,
      DeployConfig deployConfig,
      RedisData redisData,
      String cacheParameterGroupName) {

    // Set required fields
    builder.replicationGroupId(replicationGroupId);
    builder.replicationGroupDescription(deployConfig.getReplicationGroupDescription());
    builder.engine(Constants.ENGINE_TYPE);
    builder.cacheNodeType(deployConfig.getCacheNodeType());
    builder.engineVersion(deployConfig.getRedisVersion());
    builder.clusterMode(deployConfig.getClusterModeEnabled() ? "enabled" : "disabled");
    builder.transitEncryptionEnabled(deployConfig.getTransitEncryptionEnabled());

    // Set log delivery configurations if provided
    if (!deployConfig.getLogDeliveryConfigurations().isEmpty()) {
      builder.logDeliveryConfigurations(
          convertToLogDeliveryConfigurationRequests(deployConfig.getLogDeliveryConfigurations()));
    }

    // Set authentication token if enabled
    if (deployConfig.getAuthentication() != null
        && deployConfig.getAuthentication().getEnabled() != null
        && deployConfig.getAuthentication().getEnabled()) {
      if (deployConfig.getAuthentication().getAuthToken() == null
          || deployConfig.getAuthentication().getAuthToken().isEmpty()) {
        throw new GenericApplicationException(
            ApplicationError.CONSTRAINT_VIOLATION,
            "authToken is required when authentication is enabled");
      }
      // AWS requires encryption-in-transit to be enabled when using AUTH tokens
      if (!deployConfig.getTransitEncryptionEnabled()) {
        throw new GenericApplicationException(
            ApplicationError.CONSTRAINT_VIOLATION,
            "transitEncryptionEnabled must be true when authentication is enabled. "
                + "AUTH tokens are only supported when encryption-in-transit is enabled.");
      }
      builder.authToken(deployConfig.getAuthentication().getAuthToken());
    }

    // Set parameter group if provided
    ApplicationUtil.setIfNotNull(
        builder::cacheParameterGroupName, cacheParameterGroupName);

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

  /** Converts a map of tags to ElastiCache Tag objects. */
  private List<Tag> convertMapToTags(Map<String, String> tagMap) {
    return tagMap.entrySet().stream()
        .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build()).toList();
  }

  /**
   * Converts LogDeliveryConfig objects to AWS SDK LogDeliveryConfigurationRequest
   * objects.
   *
   * @param logDeliveryConfigs List of log delivery configurations from
   *                           DeployConfig
   * @return List of AWS SDK LogDeliveryConfigurationRequest objects
   */
  private List<LogDeliveryConfigurationRequest> convertToLogDeliveryConfigurationRequests(
      List<LogDeliveryConfig> logDeliveryConfigs) {
    return logDeliveryConfigs.stream()
        .map(
            config -> {
              LogDeliveryConfigurationRequest.Builder builder = LogDeliveryConfigurationRequest.builder()
                  .logType(config.getLogType().getValue())
                  .destinationType(config.getDestinationType().getValue());

              // Set destination details based on destination type
              software.amazon.awssdk.services.elasticache.model.DestinationDetails.Builder destDetailsBuilder = software.amazon.awssdk.services.elasticache.model.DestinationDetails
                  .builder();
              DestinationDetails userDestDetails = config.getDestinationDetails();

              if (config.getDestinationType() == DestinationType.CLOUDWATCH_LOGS) {
                if (userDestDetails.getCloudWatchLogsDetails() == null) {
                  throw new GenericApplicationException(
                      ApplicationError.CONSTRAINT_VIOLATION,
                      "cloudWatchLogsDetails is required when destinationType is cloudwatch-logs");
                }
                CloudWatchLogsDetails cloudWatchDetails = userDestDetails.getCloudWatchLogsDetails();
                destDetailsBuilder.cloudWatchLogsDetails(
                    CloudWatchLogsDestinationDetails.builder()
                        .logGroup(cloudWatchDetails.getLogGroup())
                        .build());
              } else if (config.getDestinationType() == DestinationType.KINESIS_FIREHOSE) {
                if (userDestDetails.getKinesisFirehoseDetails() == null) {
                  throw new GenericApplicationException(
                      ApplicationError.CONSTRAINT_VIOLATION,
                      "kinesisFirehoseDetails is required when destinationType is kinesis-firehose");
                }
                KinesisFirehoseDetails kinesisDetails = userDestDetails.getKinesisFirehoseDetails();
                destDetailsBuilder.kinesisFirehoseDetails(
                    KinesisFirehoseDestinationDetails.builder()
                        .deliveryStream(kinesisDetails.getDeliveryStream())
                        .build());
              }

              builder.destinationDetails(destDetailsBuilder.build());

              if (config.getEnabled() != null) {
                builder.enabled(config.getEnabled());
              }
              if (config.getLogFormat() != null) {
                builder.logFormat(config.getLogFormat().getValue());
              }

              return builder.build();
            })
        .toList();
  }

  /**
   * Retrieves a replication group by its identifier.
   *
   * @param replicationGroupId The identifier of the replication group
   * @return The ReplicationGroup object
   * @throws ReplicationGroupNotFoundException if the replication group is not
   *                                           found
   */
  public ReplicationGroup getReplicationGroup(String replicationGroupId) {
    DescribeReplicationGroupsRequest request = DescribeReplicationGroupsRequest.builder()
        .replicationGroupId(replicationGroupId).build();

    return elastiCacheClient.describeReplicationGroups(request).replicationGroups().stream()
        .findFirst()
        .orElseThrow(() -> new ReplicationGroupNotFoundException(replicationGroupId));
  }

  public String getReplicationGroupStatus(String replicationGroupId) {
    return getReplicationGroup(replicationGroupId).status();
  }

  /**
   * Initiates deletion of a replication group.
   *
   * @param replicationGroupId The identifier of the replication group to delete
   */
  public void deleteReplicationGroup(String replicationGroupId) {
    log.info("Initiating deletion of replication group: {}", replicationGroupId);

    elastiCacheClient.deleteReplicationGroup(
        DeleteReplicationGroupRequest.builder().replicationGroupId(replicationGroupId).build());

    log.info("Delete request submitted for replication group: {}", replicationGroupId);
  }

  /**
   * Waits for and confirms that a replication group has been successfully
   * deleted.
   * Polls the replication group status until it no longer exists or a timeout is
   * reached.
   *
   * @param replicationGroupId The identifier of the replication group to check
   * @throws GenericApplicationException if the deletion does not complete within
   *                                     the timeout period
   */
  @SneakyThrows
  public void waitForReplicationGroupDeletion(String replicationGroupId) {
    log.info("Waiting for replication group {} to be deleted...", replicationGroupId);

    long endTime = System.currentTimeMillis() + Constants.REPLICATION_GROUP_WAIT_RETRY_TIMEOUT.toMillis();

    while (System.currentTimeMillis() < endTime) {
      try {
        DescribeReplicationGroupsResponse replicationGroupResponse = elastiCacheClient.describeReplicationGroups(
            DescribeReplicationGroupsRequest.builder()
                .replicationGroupId(replicationGroupId)
                .build());

        // Check status if replication group still exists
        String status = replicationGroupResponse.replicationGroups().get(0).status();
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
}
