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
  "deployment": {
    "mode": "standalone"
  },
  "replica": {
    "count": 0
  }
}
```

### Sentinel (HA Testing)
Test high availability locally:
```json
{
  "deployment": {
    "mode": "sentinel",
    "config": {
      "replica": {
        "count": 2
      },
      "sentinel": {
    "enabled": true,
    "replicas": 3
  }
    }
  }
}
```

### Cluster Mode (Sharding Testing)
Test Redis Cluster locally:
```json
{
  "deployment": {
    "mode": "cluster",
    "config": {
    "numShards": 3,
    "replicasPerShard": 1
  }
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

| Property           | Type                        | Required | Description                                                                                                                   |
|--------------------|-----------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------|
| `deployment`       | [object](#deployment)       | **Yes**  | Deployment mode configuration determining Redis topology and high availability characteristics.                               |
| `additionalConfig` | [object](#additionalconfig) | No       | Additional Redis configuration parameters to override defaults.                                                               |
| `master`           | [object](#master)           | No       | Resource allocation and configuration for Redis master node(s). The master handles all write operations.                      |
| `nodeSelector`     | [object](#nodeselector)     | No       | Kubernetes node selector for constraining Redis pods to specific nodes.                                                       |
| `persistence`      | [object](#persistence)      | No       | Persistent storage configuration using Kubernetes PersistentVolumeClaims (PVCs). Enables data durability across pod restarts. |
| `service`          | [object](#service)          | No       | Kubernetes Service configuration controlling how Redis is exposed.                                                            |
| `updateStrategy`   | [object](#updatestrategy)   | No       | Kubernetes StatefulSet update strategy.                                                                                       |

### additionalConfig

Additional Redis configuration parameters to override defaults.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### deployment

Deployment mode configuration determining Redis topology and high availability characteristics.

#### Properties

| Property | Type              | Required                               | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|----------|-------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mode`   | string            | **Yes**                                | Redis deployment topology. **Standalone:** Single Redis instance with optional read replicas (no automatic failover) - simplest and most cost-effective for development, testing, and non-critical workloads. **Sentinel:** Master-replica topology with Sentinel processes for automatic failover (recommended for production HA). **Cluster:** Horizontal scaling with data sharding across multiple master nodes, each with replicas. Cluster mode requires `clusterModeEnabled: true` in root schema. **Default: `standalone`** (simplest, lowest cost). **Production:** Use sentinel for HA with single dataset, cluster for horizontal scaling needs. **IMPORTANT:** Changing deployment mode requires application code changes. Possible values are: `standalone`, `sentinel`, `cluster`. |
| `config` | [object](#config) | When `mode=cluster` or `mode=sentinel` | Mode-specific configuration. Contents depend on deployment.mode: standalone/sentinel use replica and sentinel objects, cluster uses numShards and replicasPerShard. See: [standalone](#config-standalone), [sentinel](#config-sentinel), [cluster](#config-cluster).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |

#### config-cluster

_Used when `mode = "cluster"`_

##### Properties

| Property           | Type   | Required | Description                                                                                                                                                                                      |
|--------------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `numShards`        | number | **Yes**  | Number of shards (master nodes) in Redis Cluster. Each shard handles a portion of the 16,384 hash slots. Minimum 3 required. **Default: `3`**. **Only valid when deployment.mode is 'cluster'**. |
| `replicasPerShard` | number | **Yes**  | Number of replicas per shard in cluster mode. Total pods = numShards × (1 + replicasPerShard). **Default: `1`**. **Only valid when deployment.mode is 'cluster'**.                               |

---

#### config-sentinel

_Used when `mode = "sentinel"`_

##### Properties

| Property   | Type                | Required | Description                                                                                                                                                                                                      |
|------------|---------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `replica`  | [object](#replica)  | **Yes**  | Replica configuration for standalone or sentinel mode. Replicas serve read operations and provide failover redundancy. In sentinel mode, replicas enable automatic failover when master fails.                   |
| `sentinel` | [object](#sentinel) | **Yes**  | Redis Sentinel configuration for high availability and automatic failover. Required when deployment.mode is 'sentinel'. Sentinel monitors master and replicas, performing automatic promotion when master fails. |

###### replica

Replica configuration for standalone or sentinel mode. Replicas serve read operations and provide failover redundancy. In sentinel mode, replicas enable automatic failover when master fails.

**Properties**

| Property    | Type                 | Required | Description                                                                                                                                                                                                                                                                                                                                                |
|-------------|----------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `count`     | number               | **Yes**  | Number of read replicas for the Redis master. Replicas provide high availability through automatic failover (when using sentinel mode) and read scaling. Each replica maintains a full copy of the data. Valid range: 0-5. Set to 0 for standalone without HA. **Default: `0`** (no replicas). **Production:** Use 1-2 replicas with sentinel mode for HA. |
| `resources` | [object](#resources) | **Yes**  | Kubernetes resource requests and limits for replica pods. Typically same as master since replicas maintain full data copies and may be promoted to master.                                                                                                                                                                                                 |

**resources**

Kubernetes resource requests and limits for replica pods. Typically same as master since replicas maintain full data copies and may be promoted to master.

**Properties**

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

**limits**

**Properties**

| Property | Type   | Required | Description                                      |
|----------|--------|----------|--------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU for replicas. **Default: `1000m`**.  |
| `memory` | string | **Yes**  | Maximum memory for replicas. **Default: `2Gi`**. |

**requests**

**Properties**

| Property | Type   | Required | Description                                                                              |
|----------|--------|----------|------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU guaranteed for replicas. **Default: `500m`**.                                |
| `memory` | string | **Yes**  | Minimum memory guaranteed for replicas. Must hold full dataset copy. **Default: `1Gi`**. |

###### sentinel

Redis Sentinel configuration for high availability and automatic failover. Required when deployment.mode is 'sentinel'. Sentinel monitors master and replicas, performing automatic promotion when master fails.

**Properties**

| Property    | Type                 | Required | Description                                                                                                                                                         |
|-------------|----------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `quorum`    | number               | **Yes**  | Minimum number of Sentinels that must agree master is down before initiating failover. Should be majority of sentinels (e.g., 2 for 3 sentinels). **Default: `2`**. |
| `replicas`  | number               | **Yes**  | Number of Sentinel processes. Must be odd number (3, 5, 7) for proper quorum. **Default: `3`**. Possible values are: `3`, `5`, `7`.                                 |
| `resources` | [object](#resources) | **Yes**  | Kubernetes resource allocation for Sentinel processes. Sentinels are lightweight, requiring minimal resources.                                                      |

**resources**

Kubernetes resource allocation for Sentinel processes. Sentinels are lightweight, requiring minimal resources.

**Properties**

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

**limits**

**Properties**

| Property | Type   | Required | Description                                        |
|----------|--------|----------|----------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU for Sentinel. **Default: `200m`**.     |
| `memory` | string | **Yes**  | Maximum memory for Sentinel. **Default: `256Mi`**. |

**requests**

**Properties**

| Property | Type   | Required | Description                                        |
|----------|--------|----------|----------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU for Sentinel. **Default: `100m`**.     |
| `memory` | string | **Yes**  | Minimum memory for Sentinel. **Default: `128Mi`**. |

---

#### config-standalone

_Used when `mode = "standalone"`_

No additional configuration required for standalone mode.

---

#### config

Mode-specific configuration. Contents depend on deployment.mode: standalone/sentinel use replica and sentinel objects, cluster uses numShards and replicasPerShard.

##### Properties

| Property           | Type                | Required             | Description                                                                                                                                                                                                      |
|--------------------|---------------------|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `numShards`        | number              | When `mode=cluster`  | Number of shards (master nodes) in Redis Cluster. Each shard handles a portion of the 16,384 hash slots. Minimum 3 required. **Default: `3`**. **Only valid when deployment.mode is 'cluster'**.                 |
| `replicasPerShard` | number              | When `mode=cluster`  | Number of replicas per shard in cluster mode. Total pods = numShards × (1 + replicasPerShard). **Default: `1`**. **Only valid when deployment.mode is 'cluster'**.                                               |
| `replica`          | [object](#replica)  | When `mode=sentinel` | Replica configuration for standalone or sentinel mode. Replicas serve read operations and provide failover redundancy. In sentinel mode, replicas enable automatic failover when master fails.                   |
| `sentinel`         | [object](#sentinel) | When `mode=sentinel` | Redis Sentinel configuration for high availability and automatic failover. Required when deployment.mode is 'sentinel'. Sentinel monitors master and replicas, performing automatic promotion when master fails. |

##### replica

Replica configuration for standalone or sentinel mode. Replicas serve read operations and provide failover redundancy. In sentinel mode, replicas enable automatic failover when master fails.

###### Properties

| Property    | Type                 | Required | Description                                                                                                                                                                                                                                                                                                                                                |
|-------------|----------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `count`     | number               | **Yes**  | Number of read replicas for the Redis master. Replicas provide high availability through automatic failover (when using sentinel mode) and read scaling. Each replica maintains a full copy of the data. Valid range: 0-5. Set to 0 for standalone without HA. **Default: `0`** (no replicas). **Production:** Use 1-2 replicas with sentinel mode for HA. |
| `resources` | [object](#resources) | **Yes**  | Kubernetes resource requests and limits for replica pods. Typically same as master since replicas maintain full data copies and may be promoted to master.                                                                                                                                                                                                 |

###### resources

Kubernetes resource requests and limits for replica pods. Typically same as master since replicas maintain full data copies and may be promoted to master.

**Properties**

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

**limits**

**Properties**

| Property | Type   | Required | Description                                      |
|----------|--------|----------|--------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU for replicas. **Default: `1000m`**.  |
| `memory` | string | **Yes**  | Maximum memory for replicas. **Default: `2Gi`**. |

**requests**

**Properties**

| Property | Type   | Required | Description                                                                              |
|----------|--------|----------|------------------------------------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU guaranteed for replicas. **Default: `500m`**.                                |
| `memory` | string | **Yes**  | Minimum memory guaranteed for replicas. Must hold full dataset copy. **Default: `1Gi`**. |

##### sentinel

Redis Sentinel configuration for high availability and automatic failover. Required when deployment.mode is 'sentinel'. Sentinel monitors master and replicas, performing automatic promotion when master fails.

###### Properties

| Property    | Type                 | Required | Description                                                                                                                                                         |
|-------------|----------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `quorum`    | number               | **Yes**  | Minimum number of Sentinels that must agree master is down before initiating failover. Should be majority of sentinels (e.g., 2 for 3 sentinels). **Default: `2`**. |
| `replicas`  | number               | **Yes**  | Number of Sentinel processes. Must be odd number (3, 5, 7) for proper quorum. **Default: `3`**. Possible values are: `3`, `5`, `7`.                                 |
| `resources` | [object](#resources) | **Yes**  | Kubernetes resource allocation for Sentinel processes. Sentinels are lightweight, requiring minimal resources.                                                      |

###### resources

Kubernetes resource allocation for Sentinel processes. Sentinels are lightweight, requiring minimal resources.

**Properties**

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

**limits**

**Properties**

| Property | Type   | Required | Description                                        |
|----------|--------|----------|----------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU for Sentinel. **Default: `200m`**.     |
| `memory` | string | **Yes**  | Maximum memory for Sentinel. **Default: `256Mi`**. |

**requests**

**Properties**

| Property | Type   | Required | Description                                        |
|----------|--------|----------|----------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU for Sentinel. **Default: `100m`**.     |
| `memory` | string | **Yes**  | Minimum memory for Sentinel. **Default: `128Mi`**. |

### master

Resource allocation and configuration for Redis master node(s). The master handles all write operations.

#### Properties

| Property    | Type                 | Required | Description                                              |
|-------------|----------------------|----------|----------------------------------------------------------|
| `resources` | [object](#resources) | **Yes**  | Kubernetes resource requests and limits for master pods. |

#### resources

Kubernetes resource requests and limits for master pods.

##### Properties

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | **Yes**  |             |
| `requests` | [object](#requests) | **Yes**  |             |

##### limits

###### Properties

| Property | Type   | Required | Description                                    |
|----------|--------|----------|------------------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU for master. **Default: `1000m`**.  |
| `memory` | string | **Yes**  | Maximum memory for master. **Default: `2Gi`**. |

##### requests

###### Properties

| Property | Type   | Required | Description                                               |
|----------|--------|----------|-----------------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU guaranteed for master. **Default: `500m`**.   |
| `memory` | string | **Yes**  | Minimum memory guaranteed for master. **Default: `1Gi`**. |

### nodeSelector

Kubernetes node selector for constraining Redis pods to specific nodes.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### persistence

Persistent storage configuration using Kubernetes PersistentVolumeClaims (PVCs). Enables data durability across pod restarts.

#### Properties

| Property       | Type           | Required | Description                                                                                                 |
|----------------|----------------|----------|-------------------------------------------------------------------------------------------------------------|
| `enabled`      | boolean        | **Yes**  | Enable persistent storage for Redis data. **Default: `true`**.                                              |
| `aof`          | [object](#aof) | No       | Append-Only File (AOF) configuration.                                                                       |
| `rdb`          | [object](#rdb) | No       | Redis Database (RDB) snapshot configuration.                                                                |
| `size`         | string         | No       | Size of persistent volume per Redis pod. **Default: `10Gi`**.                                               |
| `storageClass` | string         | No       | Kubernetes StorageClass name. When empty (default), uses cluster's default StorageClass. **Default: `""`**. |

#### aof

Append-Only File (AOF) configuration.

##### Properties

| Property      | Type    | Required | Description                                                                                 |
|---------------|---------|----------|---------------------------------------------------------------------------------------------|
| `enabled`     | boolean | No       | Enable AOF persistence. **Default: `false`**.                                               |
| `fsyncPolicy` | string  | No       | AOF fsync policy. **Default: `everysec`**. Possible values are: `always`, `everysec`, `no`. |

#### rdb

Redis Database (RDB) snapshot configuration.

##### Properties

| Property       | Type    | Required | Description                                                                          |
|----------------|---------|----------|--------------------------------------------------------------------------------------|
| `enabled`      | boolean | No       | Enable RDB snapshots. **Default: `true`**.                                           |
| `saveInterval` | string  | No       | RDB snapshot save intervals in Redis format. **Default: `"900 1 300 10 60 10000"`**. |

### service

Kubernetes Service configuration controlling how Redis is exposed.

#### Properties

| Property      | Type                   | Required | Description                                                                                                      |
|---------------|------------------------|----------|------------------------------------------------------------------------------------------------------------------|
| `annotations` | [object](#annotations) | No       | Service annotations. **Default: `{}`**.                                                                          |
| `type`        | string                 | No       | Kubernetes Service type. **Default: `ClusterIP`**. Possible values are: `ClusterIP`, `LoadBalancer`, `NodePort`. |

#### annotations

Service annotations. **Default: `{}`**.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### updateStrategy

Kubernetes StatefulSet update strategy.

#### Properties

| Property        | Type                     | Required | Description                                                                                           |
|-----------------|--------------------------|----------|-------------------------------------------------------------------------------------------------------|
| `rollingUpdate` | [object](#rollingupdate) | No       |                                                                                                       |
| `type`          | string                   | No       | Update strategy type. **Default: `RollingUpdate`**. Possible values are: `RollingUpdate`, `OnDelete`. |

#### rollingUpdate

##### Properties

| Property         | Type   | Required | Description                                               |
|------------------|--------|----------|-----------------------------------------------------------|
| `maxUnavailable` | number | No       | Maximum pods unavailable during update. **Default: `1`**. |



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
  "deployment": {
    "mode": "standalone"
  },
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
  "deployment": {
    "mode": "sentinel",
    "config": {
      "replica": {
        "count": 2
      },
      "sentinel": {
    "enabled": true,
    "replicas": 3
  }
    }
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
