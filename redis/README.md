# odin-redis-component

Deploy and manage Redis on various platforms.

## Flavors
- [aws_elasticache](aws_elasticache) - Managed Redis using AWS ElastiCache
- [aws_k8s](aws_k8s) - Self-managed Redis on AWS EKS using OpsTree Operator
- [local_k8s](local_k8s) - Self-managed Redis on local Kubernetes (kind, k3s, minikube, Docker Desktop) using OpsTree Operator

## Redis Component Definition

### Properties

| Property    | Type                 | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|-------------|----------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `discovery` | [object](#discovery) | **Yes**  | Defines how clients discover and connect to the Redis instance.                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `version`   | string               | **Yes**  | Redis engine version that determines feature availability, protocol compatibility, and performance characteristics. Newer versions offer improved performance, security patches, and additional features. Version selection impacts client library compatibility. Must be explicitly provided. **Production:** Use 7.1 or 7.0 for latest features and performance improvements; 6.2 for stability with broad client support. Possible values are: `7.2`, `7.1`, `7.0`, `6.2`, `6.0`, `5.0.6`. |

### discovery

Defines how clients discover and connect to the Redis instance.

#### Properties

| Property  | Type   | Required | Description                                                                                                                 |
|-----------|--------|----------|-----------------------------------------------------------------------------------------------------------------------------|
| `primary` | string | **Yes**  | Logical DNS name where clients connect for read/write operations. Must be provided by user based on their DNS architecture. |
| `reader`  | string | **Yes**  | Logical DNS name where clients connect for read operations. Must be provided by user based on their DNS architecture.       |


