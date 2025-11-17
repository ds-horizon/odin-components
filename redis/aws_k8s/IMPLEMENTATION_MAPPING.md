# OpsTree Operator Implementation Mapping (aws_k8s Helm Flavour)

This document maps Odin component schema properties to OpsTree Redis Operator **as implemented by the `aws_k8s` flavour**, which uses the official Opstree Helm charts (`ot-helm`) rather than writing CRDs by hand.

> **Note:** Many of the detailed CRD mappings below come from an earlier, direct‑CRD design.  
> For `aws_k8s` we now:
> - Drive Redis via Helm charts:
>   - `ot-helm/redis` (standalone)
>   - `ot-helm/redis-replication` + `ot-helm/redis-sentinel` (sentinel)
>   - `ot-helm/redis-cluster` (cluster)
> - Map Odin schema → Helm values → operator‑managed CRDs.
> - Intentionally *do not* implement some of the older mappings (e.g. `additionalRedisConfig`, `networkPolicy`, `backups`) due to known operator limitations or scope.

## Target API

**OpsTree Redis Operator CRDs:**
- API Version: `redis.redis.opstreelabs.in/v1beta2`
- Resources: `Redis`, `RedisCluster`, `RedisSentinel`, `RedisReplication`
- Documentation: https://ot-container-kit.github.io/redis-operator/

## Deployment Mode Mapping (aws_k8s)

Root schema (`redis/schema.json`):
- `version` (logical major.minor)
- `discovery` (client‑side DNS, not used to name K8s Services)

Flavour schema (`aws_k8s/schema.json`):
- `deploymentMode`: `"standalone"`, `"sentinel"`, `"cluster"`

Implementation:

| Schema Property                        | Value          | Helm Chart(s) Used                           | OpsTree CRD(s) created by operator | Notes |
|----------------------------------------|----------------|----------------------------------------------|-------------------------------------|-------|
| `deploymentMode`                       | `"standalone"` | `ot-helm/redis`                              | `Redis`                             | Single instance, no replication |
| `deploymentMode`                       | `"sentinel"`   | `ot-helm/redis-replication`, `redis-sentinel`| `RedisReplication`, `RedisSentinel` | Master–replica topology with Sentinel |
| `deploymentMode`                       | `"cluster"`    | `ot-helm/redis-cluster`                      | `RedisCluster`                      | Sharded cluster with leaders + followers |

`deploy.sh` selects the chart(s) to install based on `deploymentMode`, then waits for pods to be ready.

### Root Version Mapping (implemented)

Root `redis/schema.json`:

```json
{
  "version": {
    "enum": ["7.1", "7.0", "6.2", "6.0", "5.0.6"]
  }
}
```

For `aws_k8s` we **only support**: `6.2`, `7.0`, `7.1` with Opstree:

- `deploy.sh` validates:

  - **Allowed:** `6.2`, `7.0`, `7.1`
  - **Rejected:** `6.1` and lower (`6.0`, `5.0.6`, etc.) – operator only supports Redis ≥ 6 and is tested on ≥ 6.2.

- Values templates map the logical version to specific image tags:

  - Standalone (`values-standalone.yaml`):

    ```yaml
    redisStandalone:
      image: quay.io/opstree/redis
      tag: {% if baseConfig.version == "7.1" %}v7.1.0{% elif baseConfig.version == "7.0" %}v7.0.15{% elif baseConfig.version == "6.2" %}v6.2.14{% else %}v7.0.15{% endif %}
    ```

  - Cluster (`values-cluster.yaml`):

    ```yaml
    redisCluster:
      image: quay.io/opstree/redis
      tag: {% if baseConfig.version == "7.1" %}v7.1.0{% elif baseConfig.version == "7.0" %}v7.0.15{% elif baseConfig.version == "6.2" %}v6.2.14{% else %}v7.0.15{% endif %}
    ```

  - Sentinel (data nodes, `values-sentinel-replication.yaml`):

    ```yaml
    redisReplication:
      image: quay.io/opstree/redis
      tag: {% if baseConfig.version == "7.1" %}v7.1.0{% elif baseConfig.version == "7.0" %}v7.0.15{% elif baseConfig.version == "6.2" %}v6.2.14{% else %}v7.0.15{% endif %}
    ```

  - Sentinel (sentinel pods, `values-sentinel-sentinel.yaml`):

    ```yaml
    redisSentinel:
      image: quay.io/opstree/redis-sentinel
      tag: {% if baseConfig.version == "7.1" %}v7.1.0{% elif baseConfig.version == "7.0" %}v7.0.15{% elif baseConfig.version == "6.2" %}v6.2.14{% else %}v7.0.15{% endif %}
    ```

**Why `6.0` / `5.0.6` are not implemented for aws_k8s:** the Opstree operator only supports Redis ≥ 6 and its compatibility table and charts are focused on ≥ 6.2; earlier versions are only valid for other flavours (e.g. ElastiCache).

## Property Mappings (aws_k8s implementation)

### Basic Configuration (implemented)

| Odin Schema Property                        | Helm Values Path (aws_k8s)              | Effective CRD Field (via operator)                 | Notes |
|---------------------------------------------|-----------------------------------------|----------------------------------------------------|-------|
| `componentMetadata.envName`                 | `deploy.sh → NAMESPACE`                 | `metadata.namespace`                               | Namespace for all resources |
| `deploymentMode`                            | `deploy.sh` (switch)                    | Determines CRD kind via chart (Redis/Cluster/…)    | Chooses chart(s) to install |
| `cluster.clusterSize`                       | `values-cluster.yaml → redisCluster.clusterSize` | `RedisCluster.spec.clusterSize`            | Number of masters/shards |
| `cluster.replicasPerMaster`                 | `values-cluster.yaml → follower.replicas`        | Implied via follower replica count                | Follower replicas = `clusterSize * replicasPerMaster` |
| `sentinel.replicationSize`                  | `values-sentinel-replication.yaml → redisReplication.clusterSize` | `RedisReplication.spec.clusterSize`    | 1 master + N replicas |
| `sentinel.sentinelSize`                     | `values-sentinel-sentinel.yaml → redisSentinel.clusterSize`      | `RedisSentinel.spec.clusterSize`       | Number of sentinel pods |

### Resources, Security, Scheduling (implemented)

These are all driven from `aws_k8s/schema.json` → `flavourConfig.*` into the values files.

| Odin Schema Property                        | Values Path (standalone)                          | Values Path (cluster)                              | Values Path (sentinel data/sentinels)                            |
|---------------------------------------------|---------------------------------------------------|----------------------------------------------------|------------------------------------------------------------------|
| `resources.requests.cpu`                    | `values-standalone.yaml → redisStandalone.resources.requests.cpu` | `values-cluster.yaml → redisCluster.resources.requests.cpu` | `values-sentinel-replication.yaml → redisReplication.resources.requests.cpu` / `values-sentinel-sentinel.yaml → redisSentinel.resources.requests.cpu` |
| `resources.requests.memory`                 | same                                              | same                                               | same                                                             |
| `resources.limits.cpu`                      | same                                              | same                                               | same                                                             |
| `resources.limits.memory`                   | same                                              | same                                               | same                                                             |
| `securityContext.runAsNonRoot/runAsUser/fsGroup` | `podSecurityContext.*` in all values files  | `podSecurityContext.*`                            | `podSecurityContext.*`                                          |
| `nodeSelector`                              | `nodeSelector` map in all values files            | same                                               | same                                                             |
| `tolerations`                               | `tolerations` list in all values files            | same                                               | same                                                             |
| `priorityClassName`                         | `priorityClassName` in all values files           | same                                               | same                                                             |
| `serviceAccount`                            | `serviceAccountName` in all values files          | same                                               | same                                                             |

For cluster mode we additionally enable master–follower anti‑affinity:

```yaml
redisCluster:
  enableMasterSlaveAntiAffinity: true
```

This allows the operator’s webhook to apply pod anti‑affinity between leader and follower pods.

### Replica Resources

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `deployment.config.replica.resources.requests.cpu` | `spec.redisReplication.resources.requests.cpu` | `"500m"` | Replica CPU request (RedisReplication) |
| `deployment.config.replica.resources.requests.memory` | `spec.redisReplication.resources.requests.memory` | `"1Gi"` | Replica memory request (RedisReplication) |
| `deployment.config.replica.resources.limits.cpu` | `spec.redisReplication.resources.limits.cpu` | `"1000m"` | Replica CPU limit (RedisReplication) |
| `deployment.config.replica.resources.limits.memory` | `spec.redisReplication.resources.limits.memory` | `"2Gi"` | Replica memory limit (RedisReplication) |

### Storage & Persistence (partially implemented)

We treat `storage` from `aws_k8s/schema.json` as **PVC sizing and class**, not as a full on/off toggle:

| Odin Schema Property         | Values Path (standalone/sentinel)             | Values Path (cluster)                                        | Effective CRD Field                                    | Status |
|-----------------------------|-----------------------------------------------|--------------------------------------------------------------|--------------------------------------------------------|--------|
| `storage.storageClassName`  | `storageSpec.volumeClaimTemplate.spec.storageClassName` | same                                                     | `spec.storage.volumeClaimTemplate.spec.storageClassName` | Implemented |
| `storage.storageSize`       | `storageSpec.volumeClaimTemplate.spec.resources.requests.storage` | same                                                     | `spec.storage.volumeClaimTemplate.spec.resources.requests.storage` | Implemented |
| `storage.nodeConfStorageSize` | n/a (standalone/sentinel don’t use nodes.conf) | `storageSpec.nodeConfVolumeClaimTemplate.spec.resources.requests.storage` | `RedisCluster.spec.storage.nodeConfVolumeClaimTemplate…` | Implemented |
| `storage.enabled`           | **ignored**                                   | **ignored**                                                 | N/A                                                    | **Not implemented** |

**Reason `storage.enabled` is ignored:**  
Opstree’s operator infers persistence internally and will still try to create PVCs with `PERSISTENCE_ENABLED=true` for `RedisReplication` even if we omit `storage`. Omitting `storageSpec` leads to unbound PVCs with no `storageClassName`, not to “ephemeral only” Redis. To avoid broken PVCs we always configure `storageSpec` and document `storage.enabled` as not used for `aws_k8s`.

The more advanced `persistence` section from the generic mapping (RDB/AOF via `additionalRedisConfig`) is **not implemented** for `aws_k8s` because:

- The operator has had bugs around `additionalRedisConfig` and ConfigMap references.
- We rely on Opstree’s built‑in persistence behaviour instead of pushing custom redis.conf fragments.

So this table:

> `persistence.rdb.*`, `persistence.aof.*` → `spec.redisConfig.additionalRedisConfig`

is **intentionally not used** in the current implementation.

**Storage Example:**
```yaml
spec:
  storage:
    volumeClaimTemplate:
      spec:
        storageClassName: gp3
        accessModes:
          - ReadWriteOnce
        resources:
          requests:
            storage: 10Gi
```

**RDB and AOF Configuration Example:**

Odin Schema:
```json
{
  "persistence": {
    "enabled": true,
    "storageClass": "gp3",
    "size": "10Gi",
    "rdb": {
      "enabled": true,
      "saveInterval": "900 1 300 10 60 10000"
    },
    "aof": {
      "enabled": true,
      "fsyncPolicy": "everysec"
    }
  }
}
```

Maps to OpsTree CRD:
```yaml
spec:
  storage:
    volumeClaimTemplate:
      spec:
        storageClassName: gp3
        accessModes:
          - ReadWriteOnce
        resources:
          requests:
            storage: 10Gi
  redisConfig:
    additionalRedisConfig: |
      # RDB Configuration
      save 900 1
      save 300 10
      save 60 10000

      # AOF Configuration
      appendonly yes
      appendfsync everysec
```

**Implementation Logic:**
```python
# Pseudo-code for generating additionalRedisConfig
redis_config = []

# RDB Configuration
if persistence.rdb.enabled:
    # Parse saveInterval: "900 1 300 10 60 10000"
    # Split by pairs and generate save directives
    intervals = persistence.rdb.saveInterval.split()
    for i in range(0, len(intervals), 2):
        redis_config.append(f"save {intervals[i]} {intervals[i+1]}")
else:
    redis_config.append('save ""')  # Disable RDB

# AOF Configuration
if persistence.aof.enabled:
    redis_config.append("appendonly yes")
    redis_config.append(f"appendfsync {persistence.aof.fsyncPolicy}")
else:
    redis_config.append("appendonly no")

# Combine into additionalRedisConfig string
additional_config = "\n".join(redis_config)
```

### Sentinel Configuration (implemented via Helm charts)

`aws_k8s/schema.json` exposes a `sentinel` object:

```json
"sentinel": {
  "replicationSize": …,
  "sentinelSize": …,
  "quorum": …,
  "downAfterMilliseconds": …,
  "parallelSyncs": …,
  "failoverTimeout": …
}
```

We map these into the Opstree charts as follows:

- **Replication (`redis-replication` chart → `RedisReplication` CR):**

  ```yaml
  # values-sentinel-replication.yaml
  redisReplication:
    clusterSize: {{ flavourConfig.sentinel.replicationSize }}
  ```

- **Sentinel (`redis-sentinel` chart → `RedisSentinel` CR):**

  ```yaml
  # values-sentinel-sentinel.yaml
  redisSentinel:
    clusterSize: {{ flavourConfig.sentinel.sentinelSize }}

  redisSentinelConfig:
    redisReplicationName: {{ componentMetadata.name }}-replication
    masterGroupName: "mymaster"
    quorum: "{{ flavourConfig.sentinel.quorum }}"
    downAfterMilliseconds: "{{ flavourConfig.sentinel.downAfterMilliseconds }}"
    parallelSyncs: "{{ flavourConfig.sentinel.parallelSyncs }}"
    failoverTimeout: "{{ flavourConfig.sentinel.failoverTimeout }}"
  ```

This is different from the older “embedded sentinel in RedisReplication” mapping and reflects the current Helm‑chart‑based implementation.

**Sentinel Example:**
```yaml
apiVersion: redis.redis.opstreelabs.in/v1beta2
kind: RedisReplication
metadata:
  name: redis-replication
spec:
  size: 3  # 1 master + 2 replicas
  redisSentinel:
    replicas: 3
    config:
      quorum: "2"
    resources:
      requests:
        cpu: "100m"
        memory: "128Mi"
      limits:
        cpu: "200m"
        memory: "256Mi"
```

### Cluster Configuration (implemented via `redis-cluster` chart)

`aws_k8s/schema.json`:

```json
"cluster": {
  "clusterSize": …,
  "replicasPerMaster": …
}
```

We map this into the Helm values:

```yaml
redisCluster:
  clusterSize: {{ flavourConfig.cluster.clusterSize }}
  leader:
    replicas: {{ flavourConfig.cluster.clusterSize }}
  follower:
    replicas: {{ flavourConfig.cluster.clusterSize * flavourConfig.cluster.replicasPerMaster }}
```

The operator then creates a `RedisCluster` CR with:

- `spec.clusterSize = clusterSize`
- Leader/follower StatefulSets sized as configured.

**Cluster Example:**
```yaml
apiVersion: redis.redis.opstreelabs.in/v1beta2
kind: RedisCluster
metadata:
  name: redis-cluster
spec:
  clusterSize: 3  # Number of shards (masters)
  clusterReplicas: 1  # Replicas per shard
  kubernetesConfig:
    image: redis:7.1
    resources:
      requests:
        cpu: "1000m"
        memory: "4Gi"
```

### Metrics (implemented: exporter only)

`aws_k8s/schema.json`:

```json
"metrics": {
  "enabled": true,
  "exporterResources": { "requests": {…}, "limits": {…} }
}
```

We map this into `redisExporter` in all values files:

```yaml
redisExporter:
  enabled: {{ flavourConfig.metrics.enabled | default(true) }}
  image: quay.io/opstree/redis-exporter
  tag: "v1.44.0"
  imagePullPolicy: {{ flavourConfig.imagePullPolicy | default('IfNotPresent') }}
  resources:
    requests:
      cpu: {{ flavourConfig.metrics.exporterResources.requests.cpu | default('50m') }}
      memory: {{ flavourConfig.metrics.exporterResources.requests.memory | default('64Mi') }}
    limits:
      cpu: {{ flavourConfig.metrics.exporterResources.limits.cpu | default('200m') }}
      memory: {{ flavourConfig.metrics.exporterResources.limits.memory | default('256Mi') }}
```

**Not implemented:** `metrics.serviceMonitor.*`  
We currently **do not** create `ServiceMonitor` CRs from this component; that would be done by platform‑level tooling if needed.

**Metrics Example:**
```yaml
spec:
  redisExporter:
    enabled: true
    image: quay.io/opstree/redis-exporter:v1.44.0
    resources:
      requests:
        cpu: "50m"
        memory: "64Mi"
      limits:
        cpu: "100m"
        memory: "128Mi"
```

**ServiceMonitor Example (separate resource):**
```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: redis-metrics
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app: redis
  endpoints:
  - port: redis-exporter
    interval: 30s
```

### Pod Configuration (partially implemented)

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `antiAffinity` | `spec.affinity` | — | **Not implemented** in aws_k8s |
| `topologySpreadConstraints` | `spec.topologySpreadConstraints` | `[]` | **Not implemented** in aws_k8s |
| `nodeSelector` | `spec.nodeSelector` | `{"type": "redis"}` | Node label selector |
| `tolerations` | `spec.tolerations` | `[]` | Array of tolerations |
| `priorityClassName` | `spec.priorityClassName` | `"high-priority"` | Priority class name |
| `securityContext.runAsNonRoot` | `spec.podSecurityContext.runAsNonRoot` | `true` | Pod security context |
| `securityContext.runAsUser` | `spec.podSecurityContext.runAsUser` | `1000` | Run as user ID |
| `securityContext.fsGroup` | `spec.podSecurityContext.fsGroup` | `1000` | Filesystem group ID |

**Why `antiAffinity` / `topologySpreadConstraints` are not implemented:**  
The Opstree charts do not expose direct values for these fields, and the operator already has its own anti‑affinity knob for cluster (`enableMasterSlaveAntiAffinity`). Implementing generic `antiAffinity` / `topologySpreadConstraints` would require forking the upstream charts, which is out of scope for this component flavour.

### Service Configuration (not implemented in flavour)

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
The Opstree charts manage their own Service objects (including headless services and additional services).  
We **do not** currently surface a flavour‑level `service` block for `aws_k8s`; services use the chart defaults.

### Network Policy, PDBs, Backups, Authentication (status)

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
These sections describe potential Kubernetes resources but are **not created** by the current `aws_k8s` implementation:

- `networkPolicy.*` → no `NetworkPolicy` CRs generated.
- `podDisruptionBudget.*` → we wire `podDisruptionBudget` into **chart‑level PDB knobs** (cluster/sentinel) but we do **not** create standalone PDB CRs.
- `backup.*` → no backup `CronJob` is created; backups must be handled externally.
- Root `authentication.*` / `additionalConfig` → we do **not** create Secrets or `additionalRedisConfig` entries; authentication and extra redis.conf tuning must be done out‑of‑band if required.

**NetworkPolicy Example (separate resource):**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: redis-network-policy
  namespace: redis
spec:
  podSelector:
    matchLabels:
      app: redis
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: application
    ports:
    - protocol: TCP
      port: 6379
```

### Update Strategy

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `updateStrategy.type` | `spec.kubernetesConfig.updateStrategy.type` | `"RollingUpdate"` | Update strategy |
| `updateStrategy.rollingUpdate.maxUnavailable` | `spec.kubernetesConfig.updateStrategy.rollingUpdate.maxUnavailable` | `1` | Max unavailable pods |

### Pod Disruption Budget

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `podDisruptionBudget.enabled` | Create separate `PodDisruptionBudget` CR | - | Not in Redis CRD |
| `podDisruptionBudget.minAvailable` | `PodDisruptionBudget.spec.minAvailable` | `1` | In PDB CR |

**PodDisruptionBudget Example (separate resource):**
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: redis-pdb
  namespace: redis
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: redis
```

### Authentication

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| Root schema `authentication.enabled` | `spec.redisSecret.name` presence | `"redis-secret"` | Reference to K8s Secret |
| Root schema `authentication.authToken` | Create K8s Secret, reference in CRD | - | Secret must exist before CRD |

**Authentication Setup:**
1. Create Kubernetes Secret:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: redis-secret
  namespace: redis
type: Opaque
stringData:
  password: "your-auth-token"
```

2. Reference in Redis CRD:
```yaml
spec:
  redisSecret:
    name: redis-secret
    key: password
```

### Additional Redis Configuration

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `additionalConfig` | `spec.redisConfig.additionalRedisConfig` | `{"maxmemory-policy": "allkeys-lru"}` | Key-value pairs for redis.conf |

**Example:**
```yaml
spec:
  redisConfig:
    additionalRedisConfig:
      maxmemory-policy: "allkeys-lru"
      tcp-keepalive: "300"
      timeout: "0"
```

### Service Account

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `serviceAccount` | `spec.kubernetesConfig.serviceAccountName` | `"redis-sa"` | ServiceAccount name |

### Backups

**Note:** OpsTree Redis Operator does NOT have built-in S3 backup functionality. This must be implemented separately using:

1. **CronJob** to trigger Redis SAVE and upload to S3
2. **IRSA** (IAM Role for Service Account) for S3 access
3. **Custom backup script** running alongside Redis

**Implementation approach:**
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: redis-backup
  namespace: redis
spec:
  schedule: "0 2 * * *"  # From backup.schedule
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: redis-backup-sa  # With IRSA
          containers:
          - name: backup
            image: custom-backup-image
            env:
            - name: S3_BUCKET
              value: "my-redis-backups"  # From backup.s3Bucket
            - name: S3_REGION
              value: "us-east-1"  # From backup.s3Region
            command: ["/backup.sh"]
```

## Complete Mapping Example

### Standalone Mode

**Odin Schema:**
```json
{
  "deployment": {
    "mode": "standalone",
    "config": {}
  },
  "master": {
    "resources": {
      "requests": {"cpu": "500m", "memory": "1Gi"},
      "limits": {"cpu": "1000m", "memory": "2Gi"}
    }
  },
  "persistence": {
    "enabled": true,
    "storageClass": "gp3",
    "size": "10Gi"
  },
  "metrics": {
    "enabled": false
  }
}
```

**Note:** `namespace` comes from COMPONENT_METADATA, not flavour schema.

**OpsTree CRD:**
```yaml
apiVersion: redis.redis.opstreelabs.in/v1beta2
kind: Redis
metadata:
  name: redis-standalone
  namespace: redis
spec:
  kubernetesConfig:
    image: redis:7.1
    imagePullPolicy: IfNotPresent
    resources:
      requests:
        cpu: "500m"
        memory: "1Gi"
      limits:
        cpu: "1000m"
        memory: "2Gi"
  storage:
    volumeClaimTemplate:
      spec:
        storageClassName: gp3
        accessModes:
          - ReadWriteOnce
        resources:
          requests:
            storage: 10Gi
  redisExporter:
    enabled: false
```

### Sentinel Mode

**Odin Schema:**
```json
{
  "deployment": {
    "mode": "sentinel",
    "config": {
      "replica": {
        "count": 2,
        "resources": {
          "requests": {"cpu": "500m", "memory": "1Gi"},
          "limits": {"cpu": "1000m", "memory": "2Gi"}
        }
      },
      "sentinel": {
        "replicas": 3,
        "quorum": 2,
        "resources": {
          "requests": {"cpu": "100m", "memory": "128Mi"},
          "limits": {"cpu": "200m", "memory": "256Mi"}
        }
      }
    }
  }
}
```

**OpsTree CRD:**
```yaml
apiVersion: redis.redis.opstreelabs.in/v1beta2
kind: RedisReplication
metadata:
  name: redis-replication
  namespace: redis
spec:
  size: 3  # 1 master + deployment.config.replica.count
  kubernetesConfig:
    image: redis:7.1
  redisSentinel:
    replicas: 3
    config:
      quorum: "2"
    resources:
      requests:
        cpu: "100m"
        memory: "128Mi"
      limits:
        cpu: "200m"
        memory: "256Mi"
```

### Cluster Mode

**Odin Schema:**
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

**OpsTree CRD:**
```yaml
apiVersion: redis.redis.opstreelabs.in/v1beta2
kind: RedisCluster
metadata:
  name: redis-cluster
  namespace: redis
spec:
  clusterSize: 3
  clusterReplicas: 1
  kubernetesConfig:
    image: redis:7.1
```

## Implementation Notes

### Multiple Resource Types
Some Odin properties require creating **separate Kubernetes resources**:
- `networkPolicy.*` → Create `NetworkPolicy` CR
- `podDisruptionBudget.*` → Create `PodDisruptionBudget` CR
- `metrics.serviceMonitor.*` → Create `ServiceMonitor` CR (Prometheus Operator)
- `backup.*` → Create `CronJob` CR with custom backup script
- Root `authentication.authToken` → Create `Secret`, then reference in Redis CRD

### CRD Selection Logic
```
if deployment.mode == "standalone":
    use kind: Redis
    if deployment.config.replica.count > 0:
        set spec.size = 1 + deployment.config.replica.count (master + replicas)
elif deployment.mode == "sentinel":
    use kind: RedisReplication
    set spec.size = 1 + deployment.config.replica.count
    deployment.config must contain sentinel object
elif deployment.mode == "cluster":
    use kind: RedisCluster
    must check root schema clusterModeEnabled == true
    deployment.config must contain numShards and replicasPerShard
```

### Discovery Endpoint (documented only)
The `discovery.endpoint` from root schema should point to:
- **Standalone**: Redis master service DNS (port 6379)
- **Sentinel**: Sentinel service DNS (port 26379)
- **Cluster**: Any cluster node service DNS (port 6379)

The OpsTree operator automatically creates Kubernetes Services (e.g. `<release>-standalone`, `<release>-cluster`, sentinel/replication services).  
The root `discovery` object (and the `aws_k8s/discovery.sh` script) surface those DNS names back to the caller, but **do not** change how the Services are named in this flavour.

## References

- OpsTree Redis Operator: https://github.com/OT-CONTAINER-KIT/redis-operator
- Documentation: https://ot-container-kit.github.io/redis-operator/
- CRD Examples: https://github.com/OT-CONTAINER-KIT/redis-operator/tree/master/example
- API Reference: `redis.redis.opstreelabs.in/v1beta2`
