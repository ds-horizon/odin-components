# AWS ElastiCache Flavor

Deploy and manage Redis using AWS ElastiCache service. Defaults are optimized for quick setup with minimal cost, perfect for development and testing environments.

## Default Configuration Philosophy

This flavor uses **cost-optimized defaults** to help you get started quickly:
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

{{ .Markdown 2 }}

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

When deployed, this flavor creates:
- ElastiCache Replication Group or Cluster
- Cache Parameter Group (if specified)
- Automatic snapshots based on retention policy
- CloudWatch metrics and alarms (if configured)
