# AWS ElastiCache Flavor

Deploy and manage Redis using AWS ElastiCache service. Defaults are optimized for quick setup with minimal cost, perfect for development and testing environments.

## Default Configuration Philosophy

This flavour uses **cost-optimized defaults** to help you get started quickly:
- **Single node** setup without replicas (add replicas for production)
- **No encryption** by default (enable for production/sensitive data)
- **No automatic backups** (configure retention for production)
- **Single AZ deployment** (enable Multi-AZ for production)

## Features

- **Quick Start**: Minimal required configuration - just provide subnet and security groups
- **Cost Optimized**: Defaults minimize AWS costs for development/testing
- **Production Ready**: All production features available when needed
- **Cluster Mode Support**: Optional Redis Cluster mode for horizontal scaling
- **Flexible Scaling**: Add replicas and enable HA features as needed

## AWS ElastiCache for Redis Flavour Configuration

### Properties

| Property                      | Type                                   | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-------------------------------|----------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `atRestEncryptionEnabled`     | boolean                                | No       | Encrypts data at rest including disk-based backups, swap files, and AOF/RDB persistence files. Uses AWS-managed or customer-managed KMS keys. No performance impact but slight cost increase. Cannot be disabled after enabling. **Default: `false`** (disabled for development simplicity). **Production:** Enable for compliance (HIPAA, PCI-DSS, SOC) or sensitive data; use customer-managed KMS keys for key rotation control.                                                                                                             |
| `authentication`              | [object](#authentication)              | No       | Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.                                                                                                                                                                                                                                                                                                                                                                                                              |
| `autoMinorVersionUpgrade`     | boolean                                | No       | Automatically apply minor Redis version upgrades during maintenance windows for security patches and bug fixes. **Default: `true`** (recommended for security). Set `false` only with strict version control requirements.                                                                                                                                                                                                                                                                                                                      |
| `automaticFailoverEnabled`    | boolean                                | No       | Specifies whether a read-only replica is automatically promoted to read/write primary if the existing primary fails. AutomaticFailoverEnabled must be enabled for Redis (cluster mode enabled) replication groups. Requires at least one replica. **Default: `false`** (disabled to minimize complexity and cost for development/testing). **Production:** Enable for high availability.                                                                                                                                                        |
| `cacheNodeType`               | string                                 | No       | AWS EC2 instance type determining compute, memory, and network capacity for each Redis node. Primary driver of both performance and cost. T-series for variable workloads, M-series for general purpose, R-series for memory-intensive. ARM instances (ending in 'g') offer 20-40% better price-performance. **Default: `cache.t4g.micro`** (1 vCPU, 0.5GB RAM, suitable for dev/test). **Production:** Start with cache.m7g.large (2 vCPU, 6.38GB); use R-series for large datasets; monitor CPU/memory to right-size.                         |
| `cacheParameterGroupName`     | string                                 | No       | The name of the parameter group to associate with this replication group. If omitted, the default cache parameter group for the specified engine is used. For Redis 3.2.4+, use `default.redis3.2` (cluster mode disabled) or `default.redis3.2.cluster.on` (cluster mode enabled). Custom parameter groups enable fine-tuning of Redis settings like maxmemory-policy, eviction behaviors, etc. **Default:** AWS selects based on Redis version and cluster mode. **Production:** Create custom parameter groups for workload-specific tuning. |
| `cacheSubnetGroupName`        | string                                 | No       | The name of the ElastiCache Subnet Group, which defines the VPC and subnets for the cluster. This is fundamental for placing Redis within your private network. **Note:** If provided, this will override any cacheSubnetGroupName supplied through COMPONENT_METADATA for backward compatibility.                                                                                                                                                                                                                                              |
| `clusterModeEnabled`          | boolean                                | No       | Specifies whether to run in Redis Cluster mode for sharding and horizontal scaling. For large datasets, enabling this is recommended. **Default: `false`** (single-shard setup).                                                                                                                                                                                                                                                                                                                                                                |
| `kmsKeyId`                    | string                                 | No       | ARN of customer-managed KMS key for at-rest encryption. Only applies when `atRestEncryptionEnabled: true`. **Production:** Use customer-managed keys for compliance (HIPAA, PCI-DSS). Ensure proper key rotation policies.                                                                                                                                                                                                                                                                                                                      |
| `logDeliveryConfigurations`   | [object](#logdeliveryconfigurations)[] | No       | Log shipping configuration for slow-log and engine-log. Each object must specify `logType` (slow-log or engine-log), `destinationType` (cloudwatch-logs or kinesis-firehose), and `destinationDetails`. **Production:** Enable slow-log to CloudWatch for performance debugging.                                                                                                                                                                                                                                                                |
| `multiAzEnabled`              | boolean                                | No       | If `true`, spreads replicas across multiple Availability Zones for resilience against AZ failures. Enable for production workloads. **Default: `false`** (single AZ to minimize cost for development/testing).                                                                                                                                                                                                                                                                                                                                  |
| `notificationTopicArn`        | string                                 | No       | Amazon SNS topic ARN for receiving ElastiCache event notifications including failovers, node replacements, scaling events, maintenance, and backup completions. SNS topic owner must match cluster owner account. Enables proactive incident response. **Production:** Critical for operational visibility; create environment-specific topics; integrate with PagerDuty/Slack/email; filter events by severity.                                                                                                                                |
| `numNodeGroups`               | number                                 | No       | Number of shards (node groups) in Redis Cluster mode. Each shard handles a subset of the keyspace for horizontal scaling. Only applies when `clusterModeEnabled: true`. Increasing shards improves write throughput and data capacity linearly but increases cost and complexity. Valid range: 1-500. **Default: `1`** (single shard). **Production:** Start with 3-5 shards for high availability; scale based on write throughput needs (each shard handles ~25K writes/sec).                                                                 |
| `preferredCacheClusterAZs`    | string[]                               | No       | List of EC2 Availability Zones for placing cache nodes. Number of AZs should match total node count for even distribution. **Note:** If provided, this will override any preferredCacheClusterAZs supplied through COMPONENT_METADATA for backward compatibility. **Production:** Specify AZs close to application servers to minimize latency.                                                                                                                                                                                                 |
| `preferredMaintenanceWindow`  | string                                 | No       | The weekly time range (in UTC) for system maintenance. Format: `ddd:hh:mm-ddd:hh:mm` (e.g., `sun:04:00-sun:05:00`). Minimum window is 60 minutes. If not specified, AWS will assign one.                                                                                                                                                                                                                                                                                                                                                        |
| `replicasPerNodeGroup`        | number                                 | No       | The number of read replicas per shard. Replicas enable high availability and read scaling but increase costs. Valid range: 0-5. Set to 1 or more for production workloads. **Default: `0`** (no replicas, minimizing cost for development/testing).                                                                                                                                                                                                                                                                                             |
| `replicationGroupDescription` | string                                 | No       | Human-readable description of the replication group. Used for AWS resource management and documentation. **Default: `'ElastiCache Redis replication group'`**.                                                                                                                                                                                                                                                                                                                                                                                  |
| `securityGroupIds`            | string[]                               | No       | A list of VPC Security Group IDs that act as a virtual firewall for the cluster. This is a critical security setting that controls network access to your Redis nodes. **Note:** If provided, this will override any securityGroupIds supplied through COMPONENT_METADATA for backward compatibility.                                                                                                                                                                                                                                           |
| `snapshotRetentionLimit`      | number                                 | No       | The number of days to retain automatic backups. Backups incur storage costs. Valid range: 0-35 days. Set to 7-30 for production workloads. **Default: `0`** (no backups, minimizing storage costs for development/testing).                                                                                                                                                                                                                                                                                                                     |
| `snapshotWindow`              | string                                 | No       | Daily UTC time window for creating automated backups. Backups may impact performance during this window. Format: `HH:MM-HH:MM` in 24-hour UTC (e.g., `03:00-05:00`). Must be at least 60 minutes. Required when `snapshotRetentionLimit > 0`. AWS auto-assigns if not specified. **Production:** Schedule during lowest traffic period; monitor latency spikes during backup window; consider 02:00-04:00 UTC for US workloads.                                                                                                                 |
| `tags`                        | [object](#tags)                        | No       | A key-value map of AWS tags to apply to all created resources for cost tracking, automation, and organization. **Default: `{}`** (no tags).                                                                                                                                                                                                                                                                                                                                                                                                     |
| `transitEncryptionEnabled`    | boolean                                | No       | Enables TLS encryption for data in transit between clients and Redis nodes. Requires Redis 3.2.6+. Clients must support TLS and use port 6379 with `--tls` flag. Adds 10-20% latency overhead but essential for security. **Default: `false`** (disabled to simplify development setup). **Production:** Enable for any sensitive data, compliance requirements (HIPAA, PCI), or cross-AZ traffic.                                                                                                                                              |

### authentication

Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.

#### Properties

| Property    | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                                                               |
|-------------|---------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`   | boolean | **Yes**  | Enables Redis AUTH command for password-based access control. When true, clients must authenticate before executing commands. Adds minimal performance overhead but critical for security. **Default: `false`** (disabled for development simplicity). **Production:** Always enable for any environment with sensitive data or external access.                                          |
| `authToken` | string  | No       | Secret password required for Redis AUTH when authentication is enabled. Use strong passwords (minimum 16 characters with mixed case, numbers, special characters). Store securely using secrets management systems, never hardcode. Rotate regularly per security policies. **Production:** Use secrets manager integration (AWS Secrets Manager, HashCorp Vault) for automated rotation. |

### logDeliveryConfigurations

#### Properties

| Property             | Type                          | Required | Description                                                                             |
|----------------------|-------------------------------|----------|-----------------------------------------------------------------------------------------|
| `destinationDetails` | [object](#destinationdetails) | **Yes**  | Destination-specific configuration (log group for CloudWatch, stream for Kinesis)       |
| `destinationType`    | string                        | **Yes**  | AWS service to send logs to Possible values are: `cloudwatch-logs`, `kinesis-firehose`. |
| `logFormat`          | string                        | **Yes**  | Format of the log Possible values are: `json`, `text`.                                  |
| `logType`            | string                        | **Yes**  | Type of Redis log to capture Possible values are: `slow-log`, `engine-log`.             |
| `enabled`            | boolean                       | No       | Whether this log configuration is enabled                                               |

#### destinationDetails

Destination-specific configuration (log group for CloudWatch, stream for Kinesis)

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### tags

A key-value map of AWS tags to apply to all created resources for cost tracking, automation, and organization. **Default: `{}`** (no tags).

| Property | Type | Required | Description |
|----------|------|----------|-------------|



## Configuration Examples

### Minimal Development Configuration
```json
{
  "cacheSubnetGroupName": "my-cache-subnet-group",
  "securityGroupIds": ["sg-12345678"]
}
```
This creates a single `cache.t4g.micro` node with no replicas, no encryption, and no backups - perfect for development at minimal cost.

**Note**: `cacheSubnetGroupName` and `securityGroupIds` can also be provided via COMPONENT_METADATA for backward compatibility.

### Production-Ready Configuration
```json
{
  "cacheSubnetGroupName": "prod-cache-subnet-group",
  "securityGroupIds": ["sg-12345678", "sg-87654321"],
  "cacheNodeType": "cache.r6g.large",
  "replicasPerNodeGroup": 2,
  "automaticFailoverEnabled": true,
  "multiAzEnabled": true,
  "transitEncryptionEnabled": true,
  "atRestEncryptionEnabled": true,
  "snapshotRetentionLimit": 7,
  "snapshotWindow": "03:00-05:00",
  "preferredMaintenanceWindow": "sun:05:00-sun:06:00",
  "tags": {
    "Environment": "production",
    "Team": "platform"
  }
}
```
This creates a highly available setup with encryption, automatic failover, and backups.

### Redis Cluster Mode Configuration
```json
{
  "cacheSubnetGroupName": "my-cache-subnet-group",
  "securityGroupIds": ["sg-12345678"],
  "numNodeGroups": 3,
  "replicasPerNodeGroup": 1,
  "cacheNodeType": "cache.r6g.xlarge",
  "automaticFailoverEnabled": true,
  "multiAzEnabled": true
}
```
This creates a sharded Redis cluster for horizontal scaling. Note: `automaticFailoverEnabled` is required for cluster mode.

### Custom Parameter Group Configuration
```json
{
  "cacheSubnetGroupName": "my-cache-subnet-group",
  "securityGroupIds": ["sg-12345678"],
  "cacheParameterGroupName": "my-custom-redis7-params",
  "cacheNodeType": "cache.r6g.large",
  "replicasPerNodeGroup": 1
}
```
Use a custom parameter group when you need to tune Redis settings like maxmemory-policy, timeout, or other Redis parameters not exposed directly.

## Production Deployment Recommendations

When deploying to production, consider enabling these features:

1. **High Availability**: Set `replicasPerNodeGroup: 1` or higher and `automaticFailoverEnabled: true`
2. **Multi-AZ Resilience**: Enable `multiAzEnabled: true` to spread nodes across availability zones
3. **Encryption in Transit**: Enable `transitEncryptionEnabled: true` for sensitive data
4. **Encryption at Rest**: Enable `atRestEncryptionEnabled: true` for compliance requirements
5. **Automated Backups**: Set `snapshotRetentionLimit: 7` or higher for disaster recovery
6. **Network Security**: Use restrictive security groups and private subnets
7. **Right-size Instances**: Choose appropriate `cacheNodeType` based on workload analysis

## AWS Resources Created

When deployed, this flavour creates:
- ElastiCache Replication Group or Cluster
- Cache Parameter Group (if specified)
- Automatic snapshots based on retention policy
- CloudWatch metrics and alarms (if configured)
