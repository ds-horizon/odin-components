# OpsTree Operator Implementation Mapping

This document maps Odin component schema properties to OpsTree Redis Operator CRD fields. It serves as a reference for component developers implementing the provisioning logic.

## Target API

**OpsTree Redis Operator CRDs:**
- API Version: `redis.redis.opstreelabs.in/v1beta2`
- Resources: `Redis`, `RedisCluster`, `RedisSentinel`, `RedisReplication`
- Documentation: https://ot-container-kit.github.io/redis-operator/

## Deployment Mode Mapping

| Schema Property | Value | OpsTree CRD | Notes |
|----------------|--------|-------------|-------|
| `deploymentMode` | `"standalone"` | `kind: Redis` | Single instance, no replication |
| `deploymentMode` | `"sentinel"` | `kind: RedisReplication` | Master-replica with Sentinel |
| `deploymentMode` | `"cluster"` | `kind: RedisCluster` | Sharded cluster mode |

## Property Mappings

### Basic Configuration

| Odin Schema Property | OpsTree CRD Path | Example Value | Notes |
|---------------------|------------------|---------------|-------|
| `namespace` | `metadata.namespace` | `"redis"` | Kubernetes namespace |
| `deploymentMode` | Determines `kind` | See table above | Selects CRD type |
| `replica.count` | `spec.size` (RedisReplication) | `3` | For RedisReplication: size = 1 master + replica.count |

### Master Resources

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `master.resources.requests.cpu` | `spec.kubernetesConfig.resources.requests.cpu` | `"500m"` | Master CPU request |
| `master.resources.requests.memory` | `spec.kubernetesConfig.resources.requests.memory` | `"1Gi"` | Master memory request |
| `master.resources.limits.cpu` | `spec.kubernetesConfig.resources.limits.cpu` | `"1000m"` | Master CPU limit |
| `master.resources.limits.memory` | `spec.kubernetesConfig.resources.limits.memory` | `"2Gi"` | Master memory limit |

### Replica Resources

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `replica.resources.requests.cpu` | `spec.redisReplication.resources.requests.cpu` | `"500m"` | Replica CPU request (RedisReplication) |
| `replica.resources.requests.memory` | `spec.redisReplication.resources.requests.memory` | `"1Gi"` | Replica memory request (RedisReplication) |
| `replica.resources.limits.cpu` | `spec.redisReplication.resources.limits.cpu` | `"1000m"` | Replica CPU limit (RedisReplication) |
| `replica.resources.limits.memory` | `spec.redisReplication.resources.limits.memory` | `"2Gi"` | Replica memory limit (RedisReplication) |

### Persistence

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `persistence.enabled` | `spec.storage` presence | `{}` or omit | If enabled, include storage spec |
| `persistence.storageClass` | `spec.storage.volumeClaimTemplate.spec.storageClassName` | `"gp3"` | StorageClass name |
| `persistence.size` | `spec.storage.volumeClaimTemplate.spec.resources.requests.storage` | `"10Gi"` | Volume size |
| `persistence.rdb.enabled` | `spec.redisConfig.additionalRedisConfig` | `save 900 1` or `save ""` | Via redis.conf in additionalRedisConfig |
| `persistence.rdb.saveInterval` | `spec.redisConfig.additionalRedisConfig` | `save 900 1 300 10 60 10000` | Redis save format in additionalRedisConfig |
| `persistence.aof.enabled` | `spec.redisConfig.additionalRedisConfig` | `appendonly yes` or `appendonly no` | Via redis.conf in additionalRedisConfig |
| `persistence.aof.fsyncPolicy` | `spec.redisConfig.additionalRedisConfig` | `appendfsync everysec` | Via redis.conf in additionalRedisConfig |

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

### Sentinel Configuration

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `sentinel.enabled` | Use `RedisReplication` kind | - | Controls CRD type selection |
| `sentinel.replicas` | `spec.redisSentinel.replicas` (RedisReplication) | `3` | Number of Sentinel pods |
| `sentinel.quorum` | `spec.redisSentinel.config.quorum` (RedisReplication) | `2` | Quorum for failover |
| `sentinel.resources.requests.cpu` | `spec.redisSentinel.resources.requests.cpu` | `"100m"` | Sentinel CPU request |
| `sentinel.resources.requests.memory` | `spec.redisSentinel.resources.requests.memory` | `"128Mi"` | Sentinel memory request |
| `sentinel.resources.limits.cpu` | `spec.redisSentinel.resources.limits.cpu` | `"200m"` | Sentinel CPU limit |
| `sentinel.resources.limits.memory` | `spec.redisSentinel.resources.limits.memory` | `"256Mi"` | Sentinel memory limit |

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

### Cluster Configuration

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `cluster.numShards` | `spec.clusterSize` (RedisCluster) | `3` | Number of master nodes |
| `cluster.replicasPerShard` | `spec.clusterReplicas` (RedisCluster) | `1` | Replicas per master |

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

### Metrics

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `metrics.enabled` | `spec.redisExporter.enabled` | `true` | Enable redis-exporter sidecar |
| `metrics.serviceMonitor.enabled` | Create separate `ServiceMonitor` CR | - | Not part of Redis CRD, separate resource |
| `metrics.serviceMonitor.interval` | `ServiceMonitor.spec.interval` | `"30s"` | In ServiceMonitor CR |
| `metrics.serviceMonitor.namespace` | `ServiceMonitor.metadata.namespace` | `"monitoring"` | ServiceMonitor namespace |

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

### Pod Configuration

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `antiAffinity` | `spec.affinity` | See below | Map to PodAntiAffinity |
| `topologySpreadConstraints` | `spec.topologySpreadConstraints` | `[]` | Array of constraints |
| `nodeSelector` | `spec.nodeSelector` | `{"type": "redis"}` | Node label selector |
| `tolerations` | `spec.tolerations` | `[]` | Array of tolerations |
| `priorityClassName` | `spec.priorityClassName` | `"high-priority"` | Priority class name |
| `securityContext.runAsNonRoot` | `spec.podSecurityContext.runAsNonRoot` | `true` | Pod security context |
| `securityContext.runAsUser` | `spec.podSecurityContext.runAsUser` | `1000` | Run as user ID |
| `securityContext.fsGroup` | `spec.podSecurityContext.fsGroup` | `1000` | Filesystem group ID |

**Anti-Affinity Mapping:**
- `antiAffinity: "soft"` → `preferredDuringSchedulingIgnoredDuringExecution`
- `antiAffinity: "required"` → `requiredDuringSchedulingIgnoredDuringExecution`

**Example:**
```yaml
spec:
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - redis
        topologyKey: kubernetes.io/hostname
```

### Service Configuration

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `service.type` | `spec.kubernetesConfig.service.type` | `"ClusterIP"` | Service type |
| `service.annotations` | `spec.kubernetesConfig.service.annotations` | `{"key": "value"}` | Service annotations |

### Network Policy

| Odin Schema Property | OpsTree CRD Path | Example | Notes |
|---------------------|------------------|---------|-------|
| `networkPolicy.enabled` | Create separate `NetworkPolicy` CR | - | Not in Redis CRD |
| `networkPolicy.allowedNamespaces` | `NetworkPolicy.spec.ingress[].from` | `["app"]` | In NetworkPolicy CR |

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
  "namespace": "redis",
  "deploymentMode": "standalone",
  "replica": {
    "count": 0,
    "resources": {
      "requests": {"cpu": "500m", "memory": "1Gi"},
      "limits": {"cpu": "1000m", "memory": "2Gi"}
    }
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
  "namespace": "redis",
  "deploymentMode": "sentinel",
  "replica": {
    "count": 2,
    "resources": {
      "requests": {"cpu": "500m", "memory": "1Gi"},
      "limits": {"cpu": "1000m", "memory": "2Gi"}
    }
  },
  "sentinel": {
    "enabled": true,
    "replicas": 3,
    "quorum": 2
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
  size: 3  # 1 master + replica.count
  kubernetesConfig:
    image: redis:7.1
  redisSentinel:
    replicas: 3
    config:
      quorum: "2"
```

### Cluster Mode

**Odin Schema:**
```json
{
  "namespace": "redis",
  "deploymentMode": "cluster",
  "cluster": {
    "numShards": 3,
    "replicasPerShard": 1
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
if deploymentMode == "standalone":
    use kind: Redis
    if replica.count > 0:
        set spec.size = 1 + replica.count (master + replicas)
elif deploymentMode == "sentinel":
    use kind: RedisReplication
    set spec.size = 1 + replica.count
elif deploymentMode == "cluster":
    use kind: RedisCluster
    must check root schema clusterModeEnabled == true
    replica.count should NOT be set (validation error)
```

### Version Mapping
| Root Schema Property | OpsTree CRD Path | Format |
|---------------------|------------------|--------|
| Root `redisVersion` | `spec.kubernetesConfig.image` | `redis:{version}` |

Example: `redisVersion: "7.1"` → `image: redis:7.1`

### Discovery Endpoint
The `discovery.endpoint` from root schema should point to:
- **Standalone**: Redis master service DNS (port 6379)
- **Sentinel**: Sentinel service DNS (port 26379)
- **Cluster**: Any cluster node service DNS (port 6379)

The OpsTree operator automatically creates Kubernetes Services. The provisioning code must map these service DNS names to the discovery endpoint configuration.

## References

- OpsTree Redis Operator: https://github.com/OT-CONTAINER-KIT/redis-operator
- Documentation: https://ot-container-kit.github.io/redis-operator/
- CRD Examples: https://github.com/OT-CONTAINER-KIT/redis-operator/tree/master/example
- API Reference: `redis.redis.opstreelabs.in/v1beta2`
