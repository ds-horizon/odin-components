# odin-redis-component

Deploy and manage Redis on various platforms.

## Flavors
- [aws_elasticache](aws_elasticache)

## Redis Component Definition

### Properties

| Property             | Type                      | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|----------------------|---------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `authentication`     | [object](#authentication) | **Yes**  | Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `discovery`          | [object](#discovery)      | **Yes**  | Defines the logical DNS names where the Redis service will be accessible.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `redisVersion`       | string                    | **Yes**  | Redis engine version that determines feature availability, protocol compatibility, and performance characteristics. Newer versions offer improved performance, security patches, and additional features. Version selection impacts client library compatibility. Must be explicitly provided. **Production:** Use 7.1 or 7.0 for latest features and performance improvements; 6.2 for stability with broad client support. Possible values are: `7.1`, `7.0`, `6.2`, `6.0`, `5.0.6`.                                                                                                                                                                                                                             |
| `clusterModeEnabled` | boolean                   | No       | Specifies whether to run in Redis Cluster mode for sharding and horizontal scaling. For large datasets, enabling this is recommended. **Default: `false`** (single-shard setup).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `maxmemoryPolicy`    | string                    | No       | Eviction policy when Redis reaches memory limit. Determines which keys to remove when memory is full. Choose based on data access patterns: LRU for temporal data, LFU for frequency-based, TTL for explicit expiration, noeviction to reject writes. Performance impact varies by workload. **Default: `allkeys-lru`** (evicts least recently used keys from all keys, suitable for general caching). **Production:** Use `volatile-lru` for mixed persistent/cache data; `allkeys-lru` for pure cache; `noeviction` for persistent stores with monitoring. Possible values are: `volatile-lru`, `allkeys-lru`, `volatile-lfu`, `allkeys-lfu`, `volatile-random`, `allkeys-random`, `volatile-ttl`, `noeviction`. |

### authentication

Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.

#### Properties

| Property    | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                                                                |
|-------------|---------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`   | boolean | **Yes**  | Enables Redis AUTH command for password-based access control. When true, clients must authenticate before executing commands. Adds minimal performance overhead but critical for security. **Default: `false`** (disabled for development simplicity). **Production:** Always enable for any environment with sensitive data or external access.                                           |
| `authToken` | string  | No       | Secret password required for Redis AUTH when authentication is enabled. Use strong passwords (minimum 16 characters with mixed case, numbers, special characters). Store securely using secrets management systems, never hardcode. Rotate regularly per security policies. **Production:** Use secrets manager integration (AWS Secrets Manager, HashiCorp Vault) for automated rotation. |

### discovery

Defines the logical DNS names where the Redis service will be accessible.

#### Properties

| Property           | Type   | Required | Description                                                                                                                                                                                                                                                                                                                              |
|--------------------|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `primaryEndpoint`  | string | **Yes**  | Logical DNS name where clients connect for read/write operations. Acts as the primary entry point to your Redis instance, abstracting the underlying infrastructure. All write operations and strongly-consistent reads should use this endpoint. Must be provided by user based on their DNS architecture.                              |
| `readOnlyEndpoint` | string | No       | Logical DNS name for load-balanced read operations across replicas. Automatically distributes read traffic to reduce primary node load. Use for eventually-consistent read operations that can tolerate replication lag. **Production:** Essential for read-heavy workloads; can improve read throughput by 2-5x with multiple replicas. |



## Running locally

* Update `example/*.json` accordingly
* Download DSL jar from [artifactory](https://dreamsports.jfrog.io/ui/repos/tree/General/d11-repo/com/dream11/odin-component-interface)
* Execute the following commands
```
  export PATH_TO_JAR=<path to downloaded jar>
  bash run.sh stage=<stage> operation=<operation> account_flavour=<account_flavour>
  example:
  bash run.sh stage=deploy account_flavour=dev_aws_elasticache
```

## Contributing

* Run `bash readme-generator.sh` to auto generate README