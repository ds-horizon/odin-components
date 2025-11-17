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
Install the Redis Operator (once per local cluster):
```bash
helm repo add ot-helm https://ot-container-kit.github.io/helm-charts/
helm upgrade --install redis-operator ot-helm/redis-operator \
  --namespace redis-operator \
  --create-namespace \
  --wait
```

Verify installation:
```bash
kubectl get pods -n redis-operator
```

## Current Implementation (Helm + Opstree Operator)

This flavour uses the **same Helm + Opstree operator approach as `aws_k8s`**, but tuned for local clusters:

- `local_k8s/deploy.sh`:
  - Reads `baseConfig.version` and `flavourConfig` from `local_k8s/schema.json`.
  - Validates Redis version: **`6.2`**, **`7.0`**, **`7.2`** only.
  - Rejects `deploymentMode: "cluster"` with `version: "6.2"` (same `cluster-announce-hostname` limitation as `aws_k8s`).
  - Ensures `redis-operator` is installed via Helm in the `redis-operator` namespace.
  - Calls `helm upgrade --install` with mode‑specific values files:

    | `deploymentMode` | Helm chart(s)                      | Values file(s)                                           |
    |------------------|------------------------------------|----------------------------------------------------------|
    | `standalone`     | `ot-helm/redis`                    | `local_k8s/values-standalone.yaml`                      |
    | `sentinel`       | `ot-helm/redis-replication`        | `local_k8s/values-sentinel-replication.yaml`            |
    |                  | `ot-helm/redis-sentinel`           | `local_k8s/values-sentinel-sentinel.yaml`               |
    | `cluster`        | `ot-helm/redis-cluster`            | `local_k8s/values-cluster.yaml`                         |

  - Uses shorter Helm timeouts than `aws_k8s` (sized for laptop clusters).
  - After Helm `--wait`, runs a **pod‑level readiness check** which:
    - Counts pods by prefix (`<release>-standalone`, `<release>-cluster-leader`, etc.).
    - Ensures the expected number of pods are `Running` and all containers are `Ready`.

- `local_k8s/discovery.sh`:
  - Returns a DNS endpoint based on `deploymentMode`, identical to `aws_k8s/discovery.sh`:

    | Mode        | Service name returned                |
    |------------|---------------------------------------|
    | standalone | `${RELEASE_NAME}-standalone`          |
    | sentinel   | `${RELEASE_NAME}-replication`         |
    | cluster    | `${RELEASE_NAME}-cluster-leader`      |

  - The DNS is `<service>.<namespace>.svc.cluster.local`.

## Configuration

### Namespace

**Note:** The Kubernetes namespace for Redis deployment is provided by `COMPONENT_METADATA` and does not need to be configured in the `local_k8s` schema.

### Storage Class

For local clusters, `local_k8s/schema.json` exposes a `storage` object:

- `storage.storageClassName` (string, default `""` → use cluster default StorageClass).
- `storage.storageSize` (string, default `"1Gi"` for local).
- `storage.nodeConfStorageSize` (string, default `"256Mi"`; used only in cluster mode).

Most local distributions ship with a default StorageClass that works out of the box:

- **kind**: Uses `standard` (rancher.io/local-path)
- **k3s**: Uses `local-path`
- **minikube**: Uses `standard` (k8s.io/minikube-hostpath)
- **Docker Desktop**: Uses `hostpath`

**You typically don't need to specify `storage.storageClassName`** – leaving it empty uses the cluster default.  
Specify it only if you have multiple StorageClasses and want a particular one.

## Limitations (local_k8s flavour)

This flavour is intentionally minimal and geared towards local development/testing. Important limitations:

- **Persistence cannot be disabled**
  - The Opstree operator always uses PVCs and enables persistence internally.
  - `local_k8s` does not support an “ephemeral Redis” mode; use small `storage.storageSize` for cheap throwaway instances.

- **Cluster + Redis 6.2 is not supported**
  - `deploymentMode: "cluster"` with `baseConfig.version: "6.2"` is rejected for the same reason as `aws_k8s`: the 6.2 image does not support `cluster-announce-hostname` used by the cluster chart.

- **No built‑in backups or cloud integrations**
  - `local_k8s` does not provision backups, S3 integration, IRSA, load balancers, or CloudWatch.
  - Any persistence/backup story for local clusters should be implemented externally (e.g., scripts, Jobs).

- **Metrics via sidecar only**
  - When `metrics.enabled` is true, we add a `redis-exporter` sidecar, but we do **not** create `ServiceMonitor` or Prometheus CRDs.
  - You must configure scraping (port‑forward, Service, ServiceMonitor) yourself if you run Prometheus locally.

- **Not for production**
  - Resource defaults (`resources.*`, `storage.*`, `sentinel.*`, `cluster.*`) are sized for laptops and are not appropriate for production workloads.

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
Simplest setup for local development.  
In the current schema, this is expressed with:
- `deploymentMode: "standalone"`
- `resources` and `storage` sized for your laptop.

### Sentinel (HA Testing)
Test high availability locally with:
- `deploymentMode: "sentinel"`
- `sentinel.replicationSize` (default `2`: 1 master + 1 replica).
- `sentinel.sentinelSize` (default `3`).

### Cluster Mode (Sharding Testing)
Test Redis Cluster locally with:
- `deploymentMode: "cluster"`
- `cluster.clusterSize` (default `3` masters).
- `cluster.replicasPerMaster` (default `0` for local to keep pod count low).

## Resource Limits

For local development, you may want to reduce resource usage via the `resources` object in `local_k8s/schema.json`:

- Defaults for local clusters (approximate):
  - `resources.requests.cpu`: `"100m"`
  - `resources.requests.memory`: `"128Mi"`
  - `resources.limits.cpu`: `"500m"`
  - `resources.limits.memory`: `"512Mi"`

Increase these values only when your local cluster has enough capacity.

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

## Redis Local K8s Flavour Configuration

### Schema (current local_k8s implementation)

The `local_k8s/schema.json` file mirrors the `aws_k8s` schema but with **smaller defaults** for laptops:

| Property              | Type    | Required | Description                                                                                                  |
|-----------------------|---------|----------|--------------------------------------------------------------------------------------------------------------|
| `deploymentMode`      | string  | **Yes**  | `standalone` \| `sentinel` \| `cluster`.                                                                    |
| `cluster`             | object  | When `deploymentMode = "cluster"` | `clusterSize` (masters), `replicasPerMaster` (replicas per master).          |
| `sentinel`            | object  | When `deploymentMode = "sentinel"` | `replicationSize`, `sentinelSize`, `quorum`, and failover timers.        |
| `resources`           | object  | **Yes**  | Global CPU/memory `requests` and `limits` for all Redis pods (tuned low for local usage).                   |
| `storage`             | object  | **Yes**  | `storageClassName` (default `""`), `storageSize` (default `1Gi`), `nodeConfStorageSize` (default `256Mi`).  |
| `metrics`             | object  | No       | `enabled` (default `true`), `exporterResources` for the `redis-exporter` sidecar.                           |
| `securityContext`     | object  | No       | `runAsNonRoot`, `runAsUser`, `fsGroup` (defaults 1000).                                                     |
| `nodeSelector`        | object  | No       | Optional node selector for constraining pods.                                                               |
| `tolerations`         | array   | No       | Optional tolerations for tainted nodes.                                                                     |
| `podDisruptionBudget` | object  | No       | Hints for local PDB; disabled (`enabled: false`) by default.                                                |
| `serviceAccount`      | string  | No       | ServiceAccount name (`default` by default).                                                                 |
| `priorityClassName`   | string  | No       | Optional pod priority; usually left empty for local clusters.                                               |
| `imagePullPolicy`     | string  | No       | `Always` \| `IfNotPresent` (default) \| `Never`.                                                            |

Redis version is still taken from the **root** `redis/schema.json` (`baseConfig.version`), and is mapped to concrete image tags in the local values files:

- `6.2` → `quay.io/opstree/redis:v6.2.14`
- `7.0` → `quay.io/opstree/redis:v7.0.15`
- `7.2` → `quay.io/opstree/redis:v7.2.11`

Cluster mode with `version: "6.2"` is rejected for the same reasons as in `aws_k8s` (image does not support `cluster-announce-hostname`).

### Metrics

- When `metrics.enabled` is `true` (default for local), this flavour:
  - Adds a `redis-exporter` sidecar container to the Redis pods.
  - Uses `metrics.exporterResources.*` to size the exporter container (defaults are very small).
- The flavour does **not** create `ServiceMonitor` or other Prometheus CRDs.
  - For kind/minikube/etc., expose metrics by:
    - Port‑forwarding a Redis pod, or
    - Creating your own `Service`/`ServiceMonitor` if you run Prometheus Operator locally.

## Differences from AWS Container Flavour

See [FLAVOUR_DIFFERENCES.md](./FLAVOUR_DIFFERENCES.md) for a detailed explanation of differences between `local_k8s` and `aws_k8s`.

At a high level:
- **Same schema shape**, different defaults:
  - `resources`, `storage`, `sentinel`, `cluster`, `metrics`, etc. exist in both flavours.
  - Local defaults are much smaller (CPU/memory/storage) to fit on laptops.
- **Storage**:
  - Both flavours require persistence and PVCs.
  - `local_k8s` defaults to `storageClassName: ""` (use whatever the local cluster provides).
- **Platform integrations**:
  - `local_k8s` does not integrate with IAM/IRSA, AWS NLBs, CloudWatch, or S3 backups.
  - Multi‑AZ and advanced traffic/DR patterns are out of scope for local clusters.

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
