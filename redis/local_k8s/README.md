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

## Redis Local K8s (Opstree Operator) Flavour Configuration

Configuration schema for Redis deployment on a local Kubernetes cluster (kind/minikube/Docker Desktop) using the Opstree Redis Operator. Mirrors the aws_k8s schema shape but with smaller defaults suited for laptops.

### Properties

| Property              | Type                           | Required | Description                                                                                                                                                                                                                                                                                                                                                                       |
|-----------------------|--------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `deploymentMode`      | string                         | **Yes**  | Redis deployment mode supported by Opstree operator for local clusters. **standalone:** Single Redis instance (1 pod). **cluster:** Horizontal scaling with data sharding across multiple masters with replicas. **sentinel:** Master-replica topology with Sentinel for automatic failover. **Default: `standalone`**. Possible values are: `standalone`, `cluster`, `sentinel`. |
| `resources`           | [object](#resources)           | **Yes**  | Kubernetes resource requests and limits for Redis pods. Applies to all Redis nodes. Defaults are much smaller than aws_k8s to fit laptops.                                                                                                                                                                                                                                        |
| `storage`             | [object](#storage)             | **Yes**  | Persistent storage configuration. For local clusters you may keep this small or use ephemeral storage via the cluster's default StorageClass.                                                                                                                                                                                                                                     |
| `cluster`             | [object](#cluster)             | No       | Redis Cluster mode configuration. Only applicable when deploymentMode is 'cluster'. For local clusters you should keep sizes small (e.g. 3 masters × 1 replica).                                                                                                                                                                                                                  |
| `imagePullPolicy`     | string                         | No       | Image pull policy. **Default: `IfNotPresent`**. Possible values are: `Always`, `IfNotPresent`, `Never`.                                                                                                                                                                                                                                                                           |
| `image`               | string                         | No       | Docker image repository (including registry) for Redis data pods (standalone, cluster, replication) in local clusters. If not set, defaults to `quay.io/opstree/redis`. You can point this to your own registry, e.g. a local kind or minikube registry mirror.                                                                                                                   |
| `metrics`             | [object](#metrics)             | No       | Prometheus metrics configuration via Redis Exporter sidecar.                                                                                                                                                                                                                                                                                                                      |
| `nodeSelector`        | [object](#nodeselector)        | No       | Kubernetes node selector for constraining Redis pods to specific nodes.                                                                                                                                                                                                                                                                                                           |
| `podDisruptionBudget` | [object](#poddisruptionbudget) | No       | PodDisruptionBudget hints for local clusters.                                                                                                                                                                                                                                                                                                                                     |
| `priorityClassName`   | string                         | No       | PriorityClass name for pod scheduling priority. For local clusters this is typically left empty.                                                                                                                                                                                                                                                                                  |
| `securityContext`     | [object](#securitycontext)     | No       | Pod security context for running Redis containers with restricted privileges.                                                                                                                                                                                                                                                                                                     |
| `sentinelImage`       | string                         | No       | Docker image repository (including registry) for Redis Sentinel pods in local clusters. If not set, defaults to `quay.io/opstree/redis-sentinel`. You can point this to your own registry, e.g. a local kind or minikube registry mirror.                                                                                                                                         |
| `sentinel`            | [object](#sentinel)            | No       | Redis Sentinel mode configuration. Only applicable when deploymentMode is 'sentinel'. Provides automatic failover for master-replica topology. Defaults are reduced for local usage.                                                                                                                                                                                              |
| `serviceAccount`      | string                         | No       | Kubernetes ServiceAccount name. **Default: `default`**.                                                                                                                                                                                                                                                                                                                           |
| `tolerations`         | [object](#tolerations)[]       | No       | Pod tolerations for scheduling on tainted nodes. **Default: `[]`**.                                                                                                                                                                                                                                                                                                               |

### cluster

Redis Cluster mode configuration. Only applicable when deploymentMode is 'cluster'. For local clusters you should keep sizes small (e.g. 3 masters × 1 replica).

#### Properties

| Property            | Type    | Required | Description                                                                                                                                         |
|---------------------|---------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `clusterSize`       | integer | **Yes**  | Number of master nodes in cluster. Each master handles a portion of the 16,384 hash slots. Minimum 3 required. **Default: `3`**.                    |
| `replicasPerMaster` | integer | **Yes**  | Number of replica nodes per master. Total pods = clusterSize × (1 + replicasPerMaster). For local clusters keep this small (0–1). **Default: `0`**. |

### metrics

Prometheus metrics configuration via Redis Exporter sidecar.

#### Properties

| Property             | Type                         | Required | Description                                                                                                                                                                                       |
|----------------------|------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`            | boolean                      | No       | Enable Redis Exporter sidecar for Prometheus metrics. **Default: `true`**.                                                                                                                        |
| `exporterResources`  | [object](#exporterresources) | No       | Resource allocation for Redis Exporter sidecar container.                                                                                                                                         |
| `redisExporterImage` | string                       | No       | Docker image (including registry/repository) for the Redis Exporter sidecar in local clusters. Defaults to the public Opstree exporter image. Override this to point at your own registry mirror. |
| `redisExporterTag`   | string                       | No       | Docker image tag for the Redis Exporter sidecar. **Default: `v1.44.0`**.                                                                                                                          |

#### exporterResources

Resource allocation for Redis Exporter sidecar container.

##### Properties

| Property   | Type                | Required | Description |
|------------|---------------------|----------|-------------|
| `limits`   | [object](#limits)   | No       |             |
| `requests` | [object](#requests) | No       |             |

##### limits

###### Properties

| Property | Type   | Required | Description                                  |
|----------|--------|----------|----------------------------------------------|
| `cpu`    | string | No       | Exporter CPU limit. **Default: `100m`**.     |
| `memory` | string | No       | Exporter memory limit. **Default: `128Mi`**. |

##### requests

###### Properties

| Property | Type   | Required | Description                                   |
|----------|--------|----------|-----------------------------------------------|
| `cpu`    | string | No       | Exporter CPU request. **Default: `25m`**.     |
| `memory` | string | No       | Exporter memory request. **Default: `32Mi`**. |

### nodeSelector

Kubernetes node selector for constraining Redis pods to specific nodes.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

### podDisruptionBudget

PodDisruptionBudget hints for local clusters.

#### Properties

| Property       | Type    | Required | Description                                                                   |
|----------------|---------|----------|-------------------------------------------------------------------------------|
| `enabled`      | boolean | No       | Enable PodDisruptionBudget hints. **Default: `false`** for local.             |
| `minAvailable` | integer | No       | Minimum pods that must remain available during disruptions. **Default: `1`**. |

### resources

Kubernetes resource requests and limits for Redis pods. Applies to all Redis nodes. Defaults are much smaller than aws_k8s to fit laptops.

#### Properties

| Property   | Type                | Required | Description                   |
|------------|---------------------|----------|-------------------------------|
| `limits`   | [object](#limits)   | **Yes**  | Maximum allowed resources.    |
| `requests` | [object](#requests) | **Yes**  | Minimum guaranteed resources. |

#### limits

Maximum allowed resources.

##### Properties

| Property | Type   | Required | Description                           |
|----------|--------|----------|---------------------------------------|
| `cpu`    | string | **Yes**  | Maximum CPU. **Default: `500m`**.     |
| `memory` | string | **Yes**  | Maximum memory. **Default: `512Mi`**. |

#### requests

Minimum guaranteed resources.

##### Properties

| Property | Type   | Required | Description                                                     |
|----------|--------|----------|-----------------------------------------------------------------|
| `cpu`    | string | **Yes**  | Minimum CPU guaranteed. **Default: `100m`** for local clusters. |
| `memory` | string | **Yes**  | Minimum memory guaranteed. **Default: `128Mi`**.                |

### securityContext

Pod security context for running Redis containers with restricted privileges.

#### Properties

| Property       | Type    | Required | Description                                      |
|----------------|---------|----------|--------------------------------------------------|
| `fsGroup`      | integer | No       | GID for volume ownership. **Default: `1000`**.   |
| `runAsNonRoot` | boolean | No       | Run Redis as non-root user. **Default: `true`**. |
| `runAsUser`    | integer | No       | UID to run Redis process. **Default: `1000`**.   |

### sentinel

Redis Sentinel mode configuration. Only applicable when deploymentMode is 'sentinel'. Provides automatic failover for master-replica topology. Defaults are reduced for local usage.

#### Properties

| Property                | Type    | Required | Description                                                                                                                     |
|-------------------------|---------|----------|---------------------------------------------------------------------------------------------------------------------------------|
| `quorum`                | integer | **Yes**  | Number of Sentinels that must agree master is down before initiating failover. **Default: `2`**.                                |
| `replicationSize`       | integer | **Yes**  | Total size of replication group (1 master + N replicas). **Default: `2`** (1 master + 1 replica) for local.                     |
| `sentinelSize`          | integer | **Yes**  | Number of Sentinel instances. For local clusters we default to 3 for proper quorum. **Default: `3`**. Possible values are: `3`. |
| `downAfterMilliseconds` | integer | No       | Milliseconds before Sentinel marks an instance as down. **Default: `5000`**.                                                    |
| `failoverTimeout`       | integer | No       | Failover timeout in milliseconds. **Default: `10000`**.                                                                         |
| `parallelSyncs`         | integer | No       | Number of replicas that can sync with new master in parallel during failover. **Default: `1`**.                                 |

### storage

Persistent storage configuration. For local clusters you may keep this small or use ephemeral storage via the cluster's default StorageClass.

#### Properties

| Property              | Type   | Required | Description                                                                                                                                             |
|-----------------------|--------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `storageSize`         | string | **Yes**  | Size of persistent volume per Redis pod. **Default: `1Gi`** for local.                                                                                  |
| `nodeConfStorageSize` | string | No       | Size for cluster node configuration volume (cluster mode). **Default: `256Mi`**.                                                                        |
| `storageClassName`    | string | No       | Kubernetes StorageClass name. For local clusters this might be 'standard' or whatever your local provisioner uses. **Default: `""`** (cluster default). |

### tolerations

#### Properties

| Property   | Type   | Required | Description                                                         |
|------------|--------|----------|---------------------------------------------------------------------|
| `effect`   | string | No       | Possible values are: `NoSchedule`, `PreferNoSchedule`, `NoExecute`. |
| `key`      | string | No       |                                                                     |
| `operator` | string | No       | Possible values are: `Equal`, `Exists`.                             |
| `value`    | string | No       |                                                                     |



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
