# odin-redis-component

Deploy and manage Redis on various platforms.

## Flavors
- [aws_elasticache](aws_elasticache)

## Redis Component Definition

### Properties

| Property             | Type                      | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|----------------------|---------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `authentication`     | [object](#authentication) | **Yes**  | Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.                                                                                                                                                                                                                                                                                                                                                     |
| `discovery`          | [object](#discovery)      | **Yes**  | Defines how clients discover and connect to the Redis instance.                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `redisVersion`       | string                    | **Yes**  | Redis engine version that determines feature availability, protocol compatibility, and performance characteristics. Newer versions offer improved performance, security patches, and additional features. Version selection impacts client library compatibility. Must be explicitly provided. **Production:** Use 7.1 or 7.0 for latest features and performance improvements; 6.2 for stability with broad client support. Possible values are: `7.1`, `7.0`, `6.2`, `6.0`, `5.0.6`. |
| `clusterModeEnabled` | boolean                   | No       | Specifies whether to run in Redis Cluster mode for sharding and horizontal scaling. For large datasets, enabling this is recommended. **Default: `false`** (single-shard setup).                                                                                                                                                                                                                                                                                                       |

### authentication

Controls whether to enable the Redis AUTH command for password protection. It is highly recommended to enable this for production.

#### Properties

| Property    | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                                                               |
|-------------|---------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`   | boolean | **Yes**  | Enables Redis AUTH command for password-based access control. When true, clients must authenticate before executing commands. Adds minimal performance overhead but critical for security. **Default: `false`** (disabled for development simplicity). **Production:** Always enable for any environment with sensitive data or external access.                                          |
| `authToken` | string  | No       | Secret password required for Redis AUTH when authentication is enabled. Use strong passwords (minimum 16 characters with mixed case, numbers, special characters). Store securely using secrets management systems, never hardcode. Rotate regularly per security policies. **Production:** Use secrets manager integration (AWS Secrets Manager, HashCorp Vault) for automated rotation. |

### discovery

Defines how clients discover and connect to the Redis instance.

#### Properties

| Property   | Type   | Required | Description                                                                                                                                                                                                                                                                                                                                                                      |
|------------|--------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `endpoint` | string | **Yes**  | Logical DNS name where clients connect for read/write operations. Acts as the primary entry point to your Redis instance, abstracting the underlying infrastructure. Modern Redis clients handle read/write routing intelligently, automatically discovering replicas and routing reads appropriately in cluster mode. Must be provided by user based on their DNS architecture. |



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

* Run `bash ../readme-generator.sh` from the component directory or `bash readme-generator.sh` from repository root to auto generate README
