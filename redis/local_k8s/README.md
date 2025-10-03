# Redis Local K8s Flavour

This flavour deploys Redis on **local Kubernetes** distributions (kind, k3s, minikube, Docker Desktop, etc.) using the Opstree Redis Operator.

## Prerequisites

### Local Kubernetes Cluster
You need a local Kubernetes cluster. Choose one:

- **kind** (Kubernetes in Docker) - Recommended for testing
  ```bash
  kind create cluster --name redis-local
  ```

- **k3s** (Lightweight Kubernetes)
  ```bash
  curl -sfL https://get.k3s.io | sh -
  ```

- **minikube**
  ```bash
  minikube start
  ```

- **Docker Desktop** - Enable Kubernetes in Docker Desktop settings

### Opstree Redis Operator
Install the Redis Operator:
```bash
helm repo add ot-helm https://ot-container-kit.github.io/helm-charts/
helm install redis-operator ot-helm/redis-operator --namespace ot-operators --create-namespace
```

Verify installation:
```bash
kubectl get pods -n ot-operators
```

## Configuration

### Namespace

**Note:** The Kubernetes namespace for Redis deployment is provided by `COMPONENT_METADATA` and does not need to be configured in the flavour schema.

### Storage Class
By default, `persistence.storageClass` is empty (`""`), which means Kubernetes will automatically use your cluster's default StorageClass. This works out-of-the-box for most local Kubernetes distributions:

- **kind**: Uses `standard` (rancher.io/local-path)
- **k3s**: Uses `local-path`
- **minikube**: Uses `standard` (k8s.io/minikube-hostpath)
- **Docker Desktop**: Uses `hostpath`

**You typically don't need to specify `storageClass`** - leave it empty to use the cluster default.

Only specify explicitly if you need a non-default StorageClass:
```json
{
  "persistence": {
    "enabled": true,
    "storageClass": "local-path",  // Only if you need non-default
    "size": "10Gi"
  }
}
```

### Accessing Redis from Host Machine
For accessing Redis from your host (outside the cluster):

**Option 1: NodePort Service**
```json
{
  "service": {
    "type": "NodePort"
  }
}
```
Then connect via `localhost:<nodePort>` (get nodePort with `kubectl get svc`)

**Option 2: Port Forward**
```bash
kubectl port-forward svc/redis-master 6379:6379 -n <namespace>
```
Then connect to `localhost:6379`

## Deployment Modes

### Standalone (Development)
Simplest setup for local development:
```json
{
  "deploymentMode": "standalone",
  "replica": {
    "count": 0
  }
}
```

### Sentinel (HA Testing)
Test high availability locally:
```json
{
  "deploymentMode": "sentinel",
  "replica": {
    "count": 2
  },
  "sentinel": {
    "enabled": true,
    "replicas": 3
  }
}
```

### Cluster Mode (Sharding Testing)
Test Redis Cluster locally:
```json
{
  "deploymentMode": "cluster",
  "cluster": {
    "numShards": 3,
    "replicasPerShard": 1
  }
}
```

## Resource Limits

For local development, you may want to reduce resource usage:
```json
{
  "master": {
    "resources": {
      "requests": {
        "cpu": "250m",
        "memory": "512Mi"
      },
      "limits": {
        "cpu": "500m",
        "memory": "1Gi"
      }
    }
  }
}
```

## Troubleshooting

### Pods Stuck in Pending
Check PVC status:
```bash
kubectl get pvc -n <namespace>
```
If PVCs are pending, verify StorageClass is installed and set as default.

### Storage Class Issues
List available StorageClasses:
```bash
kubectl get storageclass
```
Update your config to use an available StorageClass.

### Connection Issues
Verify services:
```bash
kubectl get svc -n <namespace>
kubectl describe svc redis-master -n <namespace>
```

Test connection from within cluster:
```bash
kubectl run -it --rm redis-cli --image=redis:7 --restart=Never -- redis-cli -h redis-master.<namespace>.svc.cluster.local ping
```

## Redis Local K8s (Local Kubernetes) Flavour Configuration

### Properties

| Property           | Type                        | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------------|-----------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `additionalConfig` | [object](#additionalconfig) | No       | Additional Redis configuration parameters to override defaults. Supports any redis.conf setting as key-value pairs. Common parameters: maxmemory-policy (eviction), tcp-keepalive, timeout, client-output-buffer-limit. Use for advanced tuning; most settings have sensible operator defaults.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `cluster`          | [object](#cluster)          | No       | Redis Cluster mode configuration for horizontal scaling through data sharding. **IMPORTANT:** This configuration is only valid when BOTH conditions are met: (1) `clusterModeEnabled: true` in root schema AND (2) `deploymentMode: cluster` in this flavour schema. This cross-schema dependency must be validated at the configuration composition layer. Distributes 16,384 hash slots across multiple master nodes for write scaling and larger datasets.                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `deploymentMode`   | string                      | No       | Redis deployment topology determining high availability and scaling characteristics. **Standalone:** Single Redis instance with no automatic failover - simplest and most cost-effective for development, testing, and non-critical workloads. **Sentinel:** Master-replica topology with Sentinel processes for automatic failover (recommended for production HA). **Cluster:** Horizontal scaling with data sharding across multiple master nodes, each with replicas. Cluster mode requires `clusterModeEnabled: true` in root schema. **Default: `standalone`** (simplest, lowest cost for getting started). **Production:** Use sentinel for HA with single dataset, cluster for horizontal scaling needs. **IMPORTANT:** Changing deployment mode requires application code changes - see deployment modes documentation. Possible values are: `standalone`, `sentinel`, `cluster`. |
| `master`           | [object](#master)           | No       | Resource allocation and configuration for Redis master node(s). The master handles all write operations and serves as the source of truth for data replication.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `nodeSelector`     | [object](#nodeselector)     | No       | Kubernetes node selector for constraining Redis pods to specific nodes based on labels. For local k8s, useful for multi-node setups to pin Redis to specific nodes. Format: key-value pairs matching node labels.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `persistence`      | [object](#persistence)      | No       | Persistent storage configuration using Kubernetes PersistentVolumeClaims (PVCs). Enables data durability across pod restarts. Uses local storage provisioner (hostPath, local-path, or standard StorageClass depending on local k8s distribution).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `replica`          | [object](#replica)          | No       | Replica configuration for standalone or sentinel mode. Replicas serve read operations and provide failover redundancy. In sentinel mode, replicas enable automatic failover when master fails. **IMPORTANT:** This configuration is only valid for `deploymentMode: standalone` or `deploymentMode: sentinel`. For `deploymentMode: cluster`, use `cluster.replicasPerShard` instead. Configuration validation should enforce this constraint.                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `sentinel`         | [object](#sentinel)         | No       | Redis Sentinel configuration for high availability and automatic failover in sentinel deployment mode. Sentinel monitors master and replicas, performing automatic promotion when master fails. Requires at least 3 sentinel processes (odd number) for quorum.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `service`          | [object](#service)          | No       | Kubernetes Service configuration controlling how Redis is exposed for client connectivity. For local k8s, typically uses ClusterIP for internal access or NodePort for external access from host machine.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `updateStrategy`   | [object](#updatestrategy)   | No       | Kubernetes StatefulSet update strategy controlling how Redis pods are updated during version upgrades or configuration changes. Determines update order and parallelism to minimize downtime.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |

### additionalConfig

Additional Redis configuration parameters to override defaults. Supports any redis.conf setting as key-value pairs. Common parameters: maxmemory-policy (eviction), tcp-keepalive, timeout, client-output-buffer-limit. Use for advanced tuning; most settings have sensible operator defaults.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### cluster

Redis Cluster mode configuration for horizontal scaling through data sharding. **IMPORTANT:** This configuration is only valid when BOTH conditions are met: (1) `clusterModeEnabled: true` in root schema AND (2) `deploymentMode: cluster` in this flavour schema. This cross-schema dependency must be validated at the configuration composition layer. Distributes 16,384 hash slots across multiple master nodes for write scaling and larger datasets.

#### Properties

| Property           | Type   | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|--------------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `numShards`        | number | **Yes**  | Number of shards (master nodes) in the Redis Cluster. Each shard handles a portion of the 16,384 hash slots. More shards increase write throughput and total capacity but add operational complexity. Minimum 3 required for proper cluster operation. **Default: `3`** (minimum recommended for cluster mode). **Production:** Start with 3-6 shards for most use cases; scale based on write throughput needs (add shards for write scaling, add replicas for read scaling). |
| `replicasPerShard` | number | **Yes**  | Number of replicas per shard in cluster mode. Each shard (master) gets this many replicas for HA and read scaling. Total pods = numShards × (1 + replicasPerShard). Valid range: 1-5. Higher values increase availability and read throughput but multiply costs. **Default: `1`** (each shard has 1 replica for basic HA). **Production:** Use 1-2 replicas per shard; 2 for critical workloads.                                                                              |

### master

Resource allocation and configuration for Redis master node(s). The master handles all write operations and serves as the source of truth for data replication.

#### Properties

| Property    | Type                 | Required | Description                                                                                                                                                                                                                                                                                                                          |
|-------------|----------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `resources` | [object](#resources) | **Yes**  | Kubernetes resource requests and limits for master pods. Requests guarantee minimum resources; limits cap maximum usage. Setting requests equal to limits provides Guaranteed QoS (recommended for production). Memory should account for dataset size plus 30% overhead for Redis operations, replication buffers, and persistence. |

#### resources

Kubernetes resource requests and limits for master pods. Requests guarantee minimum resources; limits cap maximum usage. Setting requests equal to limits provides Guaranteed QoS (recommended for production). Memory should account for dataset size plus 30% overhead for Redis operations, replication buffers, and persistence.

##### Properties

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

##### limits

###### Properties

| Property | Type   | Required | Description                                                                                                                                                                                                                                                                        |
|----------|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU the master can use. Can be higher than requests to allow bursting. For Guaranteed QoS (production), set equal to requests. **Default: `1000m`** (1 core, 2x burst capacity). **Production:** Set equal to requests for predictable performance.                        |
| `memory` | string | **Yes**  | Maximum memory the master can use. Exceeding this triggers OOMKilled. Memory limits should be close to requests to prevent excessive swapping. **Default: `2Gi`** (2x burst capacity). **Production:** Set equal to requests for Guaranteed QoS and predictable memory allocation. |

##### requests

###### Properties

| Property | Type   | Required | Description                                                                                                                                                                                                                                                                                                                                              |
|----------|--------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU guaranteed for master. Redis is primarily single-threaded (50K ops/sec per core for simple commands). Format: millicores (e.g., '500m') or cores (e.g., '1'). **Default: `500m`** (0.5 cores, suitable for moderate workloads). **Production:** Start with 500m-1000m, monitor CPU usage, adjust based on command latency.                   |
| `memory` | string | **Yes**  | Minimum memory guaranteed for master. Should be dataset size × 1.3 to account for Redis overhead (replication buffers, persistence, fragmentation). Format: Mi or Gi (e.g., '1Gi', '512Mi'). **Default: `1Gi`** (suitable for ~700MB dataset). **Production:** Calculate as (expected dataset size × 1.3), monitor memory usage and fragmentation ratio. |

### nodeSelector

Kubernetes node selector for constraining Redis pods to specific nodes based on labels. For local k8s, useful for multi-node setups to pin Redis to specific nodes. Format: key-value pairs matching node labels.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### persistence

Persistent storage configuration using Kubernetes PersistentVolumeClaims (PVCs). Enables data durability across pod restarts. Uses local storage provisioner (hostPath, local-path, or standard StorageClass depending on local k8s distribution).

#### Properties

| Property       | Type           | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|----------------|----------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`      | boolean        | **Yes**  | Enable persistent storage for Redis data. When true, creates PVCs for each Redis pod to store RDB/AOF files. When false, uses emptyDir (data lost on pod deletion). **Default: `true`** (data durability). **Production:** Always enable for any production data.                                                                                                                                                                                                                                                                                                                                                                        |
| `aof`          | [object](#aof) | No       | Append-Only File (AOF) configuration for durable write logging. AOF logs every write operation, providing better durability than RDB. Slower to load on restart but minimal data loss (typically <1 second).                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `rdb`          | [object](#rdb) | No       | Redis Database (RDB) snapshot configuration for point-in-time backups. RDB creates binary snapshots of the dataset at specified intervals. Faster to load than AOF but may lose data between snapshots.                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `size`         | string         | No       | Size of persistent volume per Redis pod. Should be at least 2-3x expected dataset size to account for RDB snapshots, AOF files, and growth. Format: Mi, Gi, or Ti (e.g., '10Gi', '100Gi'). **Default: `10Gi`** (suitable for small datasets). **Production:** Calculate as (expected dataset size × 2.5) for RDB forks and AOF rewrites, plus growth buffer.                                                                                                                                                                                                                                                                             |
| `storageClass` | string         | No       | Kubernetes StorageClass name for local persistent volumes. When empty (default), uses the cluster's default StorageClass automatically. Common local StorageClasses: **`standard`** (default in many local k8s), **`local-path`** (Rancher local-path-provisioner for k3s/kind), **`hostpath`** (minikube default). **Default: `""` (empty - uses cluster default)**. **Note:** Most local Kubernetes distributions come with a pre-configured default StorageClass, so leaving this empty is recommended. Only specify explicitly if you need a non-default StorageClass. See FLAVOUR_DIFFERENCES.md for why this differs from aws_k8s. |

#### aof

Append-Only File (AOF) configuration for durable write logging. AOF logs every write operation, providing better durability than RDB. Slower to load on restart but minimal data loss (typically <1 second).

##### Properties

| Property      | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|---------------|---------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`     | boolean | No       | Enable AOF persistence. When true, Redis logs every write to an append-only file on disk. Provides maximum durability based on fsyncPolicy setting. Increases disk I/O and storage usage. **Default: `false`** (RDB only for dev/test simplicity). **Production:** Enable for critical data where data loss is unacceptable; disable for caching workloads where data can be regenerated.                                                                                                                                                                                                                                                                                                                                             |
| `fsyncPolicy` | string  | No       | AOF fsync policy controlling when Redis forces writes to disk. **always:** fsync after every write (safest, slowest - ensures zero data loss but high I/O overhead). **everysec:** fsync every second in background thread (recommended balance - at most 1 second of data loss, minimal performance impact). **no:** never fsync, let OS decide (fastest, riskiest - potential minutes of data loss on system crash). **Default: `everysec`** (Redis recommendation - good balance of safety and performance). **Production:** Use everysec for most workloads; always only for critical financial/transactional data where zero data loss is required; never use no in production. Possible values are: `always`, `everysec`, `no`. |

#### rdb

Redis Database (RDB) snapshot configuration for point-in-time backups. RDB creates binary snapshots of the dataset at specified intervals. Faster to load than AOF but may lose data between snapshots.

##### Properties

| Property       | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|----------------|---------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`      | boolean | No       | Enable RDB snapshots. When true, Redis periodically saves snapshots to disk based on save intervals. Provides faster restart times compared to AOF. **Default: `true`** (recommended for most use cases). **Production:** Enable for faster recovery; combine with AOF for maximum durability.                                                                                                                                                                                                                                                                                           |
| `saveInterval` | string  | No       | RDB snapshot save intervals in Redis format: `<seconds> <changes> [<seconds> <changes> ...]`. Redis saves a snapshot if at least `<changes>` keys changed in `<seconds>` seconds. Multiple intervals can be specified (space-separated). Set to empty string `""` to disable RDB. **Default: `"900 1 300 10 60 10000"`** (Redis defaults: save after 900s if 1+ keys changed, after 300s if 10+ keys changed, after 60s if 10000+ keys changed). **Production:** Adjust based on write frequency and acceptable data loss window; more frequent saves increase I/O but reduce data loss. |

### replica

Replica configuration for standalone or sentinel mode. Replicas serve read operations and provide failover redundancy. In sentinel mode, replicas enable automatic failover when master fails. **IMPORTANT:** This configuration is only valid for `deploymentMode: standalone` or `deploymentMode: sentinel`. For `deploymentMode: cluster`, use `cluster.replicasPerShard` instead. Configuration validation should enforce this constraint.

#### Properties

| Property    | Type                 | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|-------------|----------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `count`     | number               | **Yes**  | Number of read replicas for the Redis master. Replicas provide high availability through automatic failover (when using sentinel mode) and read scaling. Each replica maintains a full copy of the data. Valid range: 0-5 (limited by Opstree operator). Set to 0 for development and standalone mode (no HA). **Default: `0`** (no replicas for simplest setup). **Production:** Use 1-2 replicas with sentinel mode for HA; 3+ for read-heavy workloads. **Note:** Replicas without sentinel mode provide read scaling but NOT automatic failover. |
| `resources` | [object](#resources) | **Yes**  | Kubernetes resource requests and limits for replica pods. Typically same as master since replicas maintain full data copies and may be promoted to master. Can be lower if replicas only serve reads and promotion latency is acceptable.                                                                                                                                                                                                                                                                                                            |

#### resources

Kubernetes resource requests and limits for replica pods. Typically same as master since replicas maintain full data copies and may be promoted to master. Can be lower if replicas only serve reads and promotion latency is acceptable.

##### Properties

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

##### limits

###### Properties

| Property | Type   | Required | Description                                                                                                                     |
|----------|--------|----------|---------------------------------------------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU for replicas. **Default: `1000m`** (same as master). **Production:** Match master for consistent failover behavior. |
| `memory` | string | **Yes**  | Maximum memory for replicas. **Default: `2Gi`** (same as master). **Production:** Match master memory limits.                   |

##### requests

###### Properties

| Property | Type   | Required | Description                                                                                                                                                                              |
|----------|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU guaranteed for replicas. **Default: `500m`** (same as master for consistent performance). **Production:** Match master resources if replicas handle production read traffic. |
| `memory` | string | **Yes**  | Minimum memory guaranteed for replicas. Must hold full dataset copy. **Default: `1Gi`** (same as master). **Production:** Match master memory allocation.                                |

### sentinel

Redis Sentinel configuration for high availability and automatic failover in sentinel deployment mode. Sentinel monitors master and replicas, performing automatic promotion when master fails. Requires at least 3 sentinel processes (odd number) for quorum.

#### Properties

| Property    | Type                 | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|-------------|----------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`   | boolean              | No       | Enable Redis Sentinel for automatic failover. When true, deploys separate Sentinel processes to monitor Redis master and replicas. Required when `deploymentMode: sentinel`. Provides automatic master promotion on failure (typically 15-30 second failover time). **Default: `false`** (disabled for standalone mode by default). **Production:** Set to true when using `deploymentMode: sentinel` for automatic failover.                             |
| `quorum`    | number               | No       | Minimum number of Sentinels that must agree master is down before initiating failover. Should be majority of sentinels (e.g., 2 for 3 sentinels, 3 for 5 sentinels). Lower values enable faster failover but increase risk of false positives; higher values are more conservative. Must not exceed number of sentinel replicas. **Default: `2`** (majority for 3 sentinels). **Production:** Set to (sentinels / 2) + 1 for proper majority.             |
| `replicas`  | number               | No       | Number of Sentinel processes. Must be odd number (3, 5, 7) for proper quorum and split-brain prevention. Sentinels vote on failover decisions; quorum determines minimum agreeing sentinels for promotion. More sentinels increase availability but add resource overhead. **Default: `3`** (minimum recommended for production HA). **Production:** 3 sentinels sufficient for most cases; 5 for critical workloads. Possible values are: `3`, `5`, `7`. |
| `resources` | [object](#resources) | No       | Kubernetes resource allocation for Sentinel processes. Sentinels are lightweight (primarily network I/O and health checks), requiring minimal resources compared to Redis nodes.                                                                                                                                                                                                                                                                          |

#### resources

Kubernetes resource allocation for Sentinel processes. Sentinels are lightweight (primarily network I/O and health checks), requiring minimal resources compared to Redis nodes.

##### Properties

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

##### limits

###### Properties

| Property | Type   | Required | Description                                                                                                                     |
|----------|--------|----------|---------------------------------------------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU for Sentinel processes. **Default: `200m`** (2x burst capacity). **Production:** 200m provides adequate headroom.   |
| `memory` | string | **Yes**  | Maximum memory for Sentinel processes. **Default: `256Mi`** (2x base allocation). **Production:** 256Mi adequate with headroom. |

##### requests

###### Properties

| Property | Type   | Required | Description                                                                                                                                          |
|----------|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU for Sentinel processes. **Default: `100m`** (0.1 cores, sufficient for monitoring). **Production:** 100m adequate for most deployments.  |
| `memory` | string | **Yes**  | Minimum memory for Sentinel processes. **Default: `128Mi`** (sufficient for Sentinel state). **Production:** 128Mi adequate for typical deployments. |

### service

Kubernetes Service configuration controlling how Redis is exposed for client connectivity. For local k8s, typically uses ClusterIP for internal access or NodePort for external access from host machine.

#### Properties

| Property      | Type                   | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|---------------|------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `annotations` | [object](#annotations) | No       | Kubernetes Service annotations for additional configuration. For local k8s, typically not needed unless using specific local load balancer implementations (e.g., MetalLB). **Default: `{}`** (no annotations).                                                                                                                                                                                                                                                                                                                                                                                      |
| `type`        | string                 | No       | Kubernetes Service type determining Redis accessibility. **ClusterIP:** Internal cluster access only via cluster DNS (redis-master.<namespace>.svc.cluster.local). **NodePort:** Exposes on each node's IP at static port (useful for local access from host). **LoadBalancer:** Creates external LB (behavior depends on local k8s setup - may create external IP or be equivalent to NodePort). **Default: `ClusterIP`** (internal access). **Local setup:** Use NodePort for access from host machine (e.g., localhost:<nodePort>). Possible values are: `ClusterIP`, `LoadBalancer`, `NodePort`. |

#### annotations

Kubernetes Service annotations for additional configuration. For local k8s, typically not needed unless using specific local load balancer implementations (e.g., MetalLB). **Default: `{}`** (no annotations).

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### updateStrategy

Kubernetes StatefulSet update strategy controlling how Redis pods are updated during version upgrades or configuration changes. Determines update order and parallelism to minimize downtime.

#### Properties

| Property        | Type                     | Required | Description                                                                                                                                                                                                                                                                                                                                  |
|-----------------|--------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `rollingUpdate` | [object](#rollingupdate) | No       |                                                                                                                                                                                                                                                                                                                                              |
| `type`          | string                   | No       | Update strategy type. **RollingUpdate:** Automatically updates pods in reverse ordinal order (replicas before master in StatefulSets). Opstree operator manages graceful updates. **OnDelete:** Pods updated only when manually deleted. **Default: `RollingUpdate`** (automated updates). Possible values are: `RollingUpdate`, `OnDelete`. |

#### rollingUpdate

##### Properties

| Property         | Type   | Required | Description                                                                                                                                                                                                              |
|------------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `maxUnavailable` | number | No       | Maximum number of pods unavailable during update. For StatefulSets, typically 1 (update one pod at a time). Combined with PodDisruptionBudget ensures availability during updates. **Default: `1`** (one pod at a time). |



## Differences from AWS Container Flavour

**Why can't we symlink to aws_k8s schema?**
See [FLAVOUR_DIFFERENCES.md](./FLAVOUR_DIFFERENCES.md) for detailed explanation of differences between local_k8s and aws_k8s flavours.

Key differences:
- **Storage**: Both use empty storageClass (cluster default), but local K8s has pre-configured defaults while EKS 1.30+ requires external StorageClass setup
- **Setup Required**: None (pre-configured defaults) vs Must create and configure default StorageClass
- **Load Balancing**: NodePort/port-forward vs AWS NLB
- **IAM**: No IRSA integration locally
- **Multi-AZ**: Single node/zone vs multi-AZ spreading
- **Backups**: Not supported vs Automated S3 backups

## Example Configurations

### Minimal Development Setup
```json
{
  "deploymentMode": "standalone",
  "persistence": {
    "enabled": true,
    "size": "5Gi"
  },
  "service": {
    "type": "ClusterIP"
  }
}
```
**Note:** `storageClass` is omitted - uses cluster default automatically.

### Local HA Testing Setup
```json
{
  "deploymentMode": "sentinel",
  "replica": {
    "count": 2
  },
  "sentinel": {
    "enabled": true,
    "replicas": 3
  },
  "persistence": {
    "enabled": true,
    "size": "10Gi"
  }
}
```
**Note:** `storageClass` is omitted - uses cluster default automatically.

## Cleanup

Delete Redis deployment:
```bash
kubectl delete -n <namespace> redis <instance-name>
```

Delete PVCs (data will be lost):
```bash
kubectl delete pvc -n <namespace> --all
```

Delete operator:
```bash
helm uninstall redis-operator -n ot-operators
```

Delete cluster:
```bash
# kind
kind delete cluster --name redis-local

# k3s
sudo /usr/local/bin/k3s-uninstall.sh

# minikube
minikube delete
```
