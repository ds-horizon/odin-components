# AWS K8s (EKS) Flavour

Deploy and manage Redis on AWS EKS using the [Opstree Redis Operator](https://github.com/OT-CONTAINER-KIT/redis-operator). Defaults are optimized for simplicity and cost-effectiveness, perfect for getting started quickly. Easily upgrade to Sentinel or Cluster mode for production workloads requiring high availability or horizontal scaling.

{{ .Markdown 2 }}

## Default Configuration Philosophy

This flavour uses **standalone defaults** optimized for simplicity and cost:
- **Standalone mode** - single Redis instance (no HA, simplest setup)
- **No replicas** by default (replicaCount: 0)
- **Persistent storage** enabled with gp3 volumes (AWS EBS)
- **No metrics** by default (enable Prometheus metrics for production)
- **No backups** by default (configure S3 backups for production)
- **ClusterIP service** for internal cluster access only

**When to upgrade**:
- [**Sentinel mode**](#sentinel-mode-recommended-for-production): When you need automatic failover for production
- [**Cluster mode**](#cluster-mode-horizontal-scaling): When dataset >50GB or need horizontal write scaling

## Features

- **Simple by Default**: Standalone mode for quick starts, minimal configuration
- **Scalable**: Upgrade to Sentinel (HA) or Cluster (horizontal scaling) when needed
- **Kubernetes-Native**: Leverages Kubernetes primitives (StatefulSets, PVCs, Services)
- **Operator-Managed**: Opstree Redis Operator handles lifecycle, scaling, and healing
- **Multi-AZ Support**: Configure topology spread constraints for zone distribution
- **Full Observability**: Prometheus metrics and Grafana dashboards
- **Automated Backups**: Optional S3 backups with configurable retention

## For Component Developers & Contributors

**Implementation Reference:** If you're developing provisioning logic or contributing to this component, see [IMPLEMENTATION_MAPPING.md](IMPLEMENTATION_MAPPING.md) for detailed mappings between Odin schema properties and OpsTree Redis Operator CRD fields.

## Prerequisites (For Odin Admins)

Before enabling usage of this flavour of Redis, ensure:

1. **EKS Cluster**: Kubernetes 1.18+ cluster running on AWS EKS
2. **Opstree Redis Operator**: Installed in your cluster
   ```bash
   helm repo add ot-helm https://ot-container-kit.github.io/helm-charts/
   helm install redis-operator ot-helm/redis-operator -n redis-operator --create-namespace
   ```
3. **AWS EBS CSI Driver**: Installed for persistent volume provisioning (required for gp3 volumes)
   ```bash
   kubectl get deployment ebs-csi-controller -n kube-system
   ```
4. **Storage Class**: Recommended to create `gp3` StorageClass and set as default

   **IMPORTANT:** EKS 1.30+ does not include a default StorageClass. You must either:
   - Create a StorageClass and mark it as default, OR
   - Explicitly specify `storageClass` in your Redis configuration

   **Create gp3 StorageClass:**
   ```bash
   kubectl apply -f - <<EOF
   apiVersion: storage.k8s.io/v1
   kind: StorageClass
   metadata:
     name: gp3
     annotations:
       storageclass.kubernetes.io/is-default-class: "true"
   provisioner: ebs.csi.aws.com
   parameters:
     type: gp3
     encrypted: "true"  # Recommended for production
   volumeBindingMode: WaitForFirstConsumer
   allowVolumeExpansion: true
   EOF
   ```

   Verify:
   ```bash
   kubectl get storageclass
   ```

5. **Optional - Prometheus Operator**: For ServiceMonitor-based metrics collection
6. **Optional - IAM Role for Service Account (IRSA)**: For S3 backup access

## Configuration

### Namespace

**Note:** The Kubernetes namespace for Redis deployment is provided by `COMPONENT_METADATA` and does not need to be configured in the flavour schema.

## Configuration Examples

### Minimal Development Configuration (Default)
```json
{}
```
This uses all defaults: standalone mode with a single Redis instance - perfect for development with minimal resources (1 pod).

**Cost Estimate**: ~$30-50/month (t3.medium node, 10GB gp3 storage)

**Application Connection**:
```javascript
// Standard Redis connection
const redis = new Redis({
  host: 'discovery.endpoint',  // From root schema
  port: 6379,
  password: 'your-auth-token'
});
```

### Custom Storage for Development
```json
{
  "persistence": {
    "size": "5Gi"
  }
}
```
Reduces storage to 5GB for even lower costs.

**Cost Estimate**: ~$25-40/month

### Production Sentinel with Multi-AZ
```json
{
  "deployment": {
    "mode": "sentinel",
    "config": {
  "replicaCount": 2,
  "master": {
    "resources": {
      "requests": { "cpu": "1000m", "memory": "2Gi" },
      "limits": { "cpu": "2000m", "memory": "4Gi" }
    }
  },
  "replica": {
    "resources": {
      "requests": { "cpu": "1000m", "memory": "2Gi" },
      "limits": { "cpu": "2000m", "memory": "4Gi" }
    }
  },
  "persistence": {
    "enabled": true,
    "storageClass": "gp3-encrypted",
    "size": "50Gi",
    "aof": {
      "enabled": true
    }
  },
  "sentinel": {
    "enabled": true,
    "replicas": 3,
    "quorum": 2
  },
  "antiAffinity": "required",
  "topologySpreadConstraints": [
    {
      "maxSkew": 1,
      "topologyKey": "topology.kubernetes.io/zone",
      "whenUnsatisfiable": "DoNotSchedule"
    }
  ],
  "metrics": {
    "enabled": true,
    "serviceMonitor": {
      "enabled": true,
      "interval": "30s"
    }
  },
  "backup": {
    "enabled": true,
    "schedule": "0 2 * * *",
    "s3Bucket": "my-redis-backups",
    "s3Region": "us-east-1",
    "retention": 30
  },
  "podDisruptionBudget": {
    "enabled": true,
    "minAvailable": 2
  }
}
```
This creates a highly available production setup:
- 1 master + 2 replicas (spread across 3 AZs)
- Encrypted storage (gp3-encrypted StorageClass for at-rest encryption)
- AOF persistence for durability
- Required anti-affinity (strict pod spreading)
- Automated daily S3 backups (30-day retention)
- Prometheus monitoring with ServiceMonitor
- PodDisruptionBudget ensures 2 pods always available

**Cost Estimate**: ~$300-400/month (3 m5.xlarge nodes, 150GB gp3, S3 backups)

### Redis Cluster Mode (Horizontal Scaling)
```json
{
  "deployment": {
    "mode": "cluster",
    "config": {
  "cluster": {
    "numShards": 3,
    "replicasPerShard": 1
  },
  "master": {
    "resources": {
      "requests": { "cpu": "1000m", "memory": "4Gi" },
      "limits": { "cpu": "2000m", "memory": "8Gi" }
    }
  },
  "replica": {
    "resources": {
      "requests": { "cpu": "1000m", "memory": "4Gi" },
      "limits": { "cpu": "2000m", "memory": "8Gi" }
    }
  },
  "persistence": {
    "enabled": true,
    "size": "100Gi"
  },
  "antiAffinity": "required",
  "topologySpreadConstraints": [
    {
      "maxSkew": 1,
      "topologyKey": "topology.kubernetes.io/zone",
      "whenUnsatisfiable": "DoNotSchedule"
    }
  ],
  "metrics": {
    "enabled": true,
    "serviceMonitor": {
      "enabled": true
    }
  }
}
```
This creates a sharded Redis Cluster for horizontal scaling:
- 3 shards (masters) + 3 replicas = 6 nodes total
- Each shard handles ~33% of keyspace (16,384 hash slots distributed)
- Scales write throughput linearly (~75K writes/sec total)
- Spread across multiple AZs for resilience
- Note: Requires `clusterModeEnabled: true` in root schema

**Cost Estimate**: ~$600-800/month (6 r5.large nodes, 600GB gp3)

### External Access via Network Load Balancer
```json
{
  "service": {
    "type": "LoadBalancer",
    "annotations": {
      "service.beta.kubernetes.io/aws-load-balancer-type": "nlb",
      "service.beta.kubernetes.io/aws-load-balancer-internal": "true",
      "service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled": "true"
    }
  }
}
```
Exposes Redis via internal AWS Network Load Balancer for cross-VPC or on-premises access. Use internal NLB for security.

**Additional Cost**: ~$16-20/month (NLB charges)

### Network Policy for Zero-Trust Security
```json
{
  "networkPolicy": {
    "enabled": true,
    "allowedNamespaces": ["application", "backend-services"]
  }
}
```
Restricts Redis access to only specified namespaces using Kubernetes NetworkPolicy. Requires CNI with NetworkPolicy support (Calico, AWS VPC CNI with network policy).

### S3 Backups with IRSA
```json
{
  "serviceAccount": "redis-backup-sa",
  "backup": {
    "enabled": true,
    "schedule": "0 */6 * * *",
    "s3Bucket": "prod-redis-backups",
    "s3Region": "us-east-1",
    "retention": 90
  }
}
```
Configures automated backups every 6 hours to S3 with 90-day retention. Requires:
1. Create S3 bucket with versioning enabled
2. Create IAM role with S3 write permissions
3. Create ServiceAccount annotated with IAM role ARN (IRSA)
   ```bash
   eksctl create iamserviceaccount \
     --name redis-backup-sa \
     --namespace <namespace> \
     --cluster my-cluster \
     --attach-policy-arn arn:aws:iam::ACCOUNT:policy/RedisS3BackupPolicy \
     --approve
   ```

## Deployment Modes

**IMPORTANT**: Each deployment mode requires **different application code** for connecting to Redis. Choose your mode based on requirements, then configure your application accordingly.

### Standalone Mode (Default)
**Use Case**: Development, testing, non-critical caching, single-tenant applications

**Configuration**:
```json
{
  "deployment": {
    "mode": "standalone"
  },
  "replicaCount": 0
}
```

**Characteristics**:
- Single Redis instance (1 pod)
- No high availability or failover
- Simplest setup with minimal resources
- Pod restart = brief downtime (5-30 seconds)

**Cost**: Lowest (~$30-60/month)

**Application Code** (Standard Redis Connection):

**Node.js (ioredis)**:
```javascript
const Redis = require('ioredis');

const redis = new Redis({
  host: process.env.REDIS_HOST,  // discovery.endpoint DNS
  port: 6379,
  password: process.env.REDIS_PASSWORD,
  retryStrategy: (times) => Math.min(times * 50, 2000)
});
```

**Python (redis-py)**:
```python
import redis
import os

r = redis.Redis(
    host=os.getenv('REDIS_HOST'),  # discovery.endpoint DNS
    port=6379,
    password=os.getenv('REDIS_PASSWORD'),
    decode_responses=True,
    socket_connect_timeout=5,
    retry_on_timeout=True
)
```

**Java (Jedis)**:
```java
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

JedisPoolConfig poolConfig = new JedisPoolConfig();
poolConfig.setMaxTotal(50);

JedisPool pool = new JedisPool(
    poolConfig,
    System.getenv("REDIS_HOST"),  // discovery.endpoint DNS
    6379,
    2000,
    System.getenv("REDIS_PASSWORD")
);

try (Jedis jedis = pool.getResource()) {
    jedis.set("key", "value");
}
```

**Go (go-redis)**:
```go
import (
    "github.com/redis/go-redis/v9"
    "os"
)

rdb := redis.NewClient(&redis.Options{
    Addr:     os.Getenv("REDIS_HOST") + ":6379",  // discovery.endpoint DNS
    Password: os.Getenv("REDIS_PASSWORD"),
    DB:       0,
})
```

---

### Sentinel Mode (Recommended for Production)
**Use Case**: Production HA, single dataset, automatic failover

**Configuration**:
```json
{
  "deployment": {
    "mode": "sentinel",
    "config": {
  "replicaCount": 2,
  "sentinel": {
    "enabled": true,
    "replicas": 3,
    "quorum": 2
  }
}
```

**Characteristics**:
- 1 master + N replicas (typically 1-2 replicas)
- 3-7 Sentinel processes monitor health (default: 3)
- Automatic failover when master fails (15-30 seconds)
- Replicas serve read operations
- Best for datasets that fit on single node

**IMPORTANT**: `discovery.endpoint` in root schema should point to **Sentinel service DNS** (not Redis master)

**Architecture**:
```
┌─────────────────────────────────────────┐
│           Sentinel Processes            │
│  (Monitor master/replicas, vote on     │
│   failover, update clients on topology) │
│                                         │
│  Sentinel-0   Sentinel-1   Sentinel-2  │
│    (AZ-1a)      (AZ-1b)      (AZ-1c)   │
└─────────────────────────────────────────┘
              │         │         │
              ▼         ▼         ▼
    ┌─────────────────────────────────┐
    │         Redis Nodes             │
    │                                 │
    │  Master        Replica          │
    │  (AZ-1a)       (AZ-1b)          │
    │  [writes]      [reads]          │
    └─────────────────────────────────┘
```

**Failover Process**:
1. Master becomes unresponsive (network/crash/AZ failure)
2. Sentinels detect failure after timeout (~10s)
3. Quorum of Sentinels vote for failover (2 of 3 agree)
4. Replica promoted to new master (~5s)
5. Sentinels update clients with new master address
6. Total downtime: 15-30 seconds

**Cost**: Medium (~$150-300/month)

**Application Code** (Sentinel-Aware Connection):

**Node.js (ioredis)**:
```javascript
const Redis = require('ioredis');

const redis = new Redis({
  sentinels: [
    { host: process.env.REDIS_SENTINEL_HOST, port: 26379 },  // discovery.endpoint DNS
    // ioredis automatically discovers other sentinels
  ],
  name: 'mymaster',  // Redis master name (check operator config)
  password: process.env.REDIS_PASSWORD,
  sentinelPassword: process.env.REDIS_PASSWORD,  // If sentinel auth enabled
  retryStrategy: (times) => Math.min(times * 50, 2000),
  sentinelRetryStrategy: (times) => Math.min(times * 50, 2000)
});

// ioredis automatically handles failover - no app changes needed
redis.on('error', (err) => console.error('Redis error:', err));
redis.on('+switch-master', (info) => console.log('Master switched:', info));
```

**Python (redis-py with sentinel)**:
```python
from redis.sentinel import Sentinel
import os

sentinel = Sentinel(
    [(os.getenv('REDIS_SENTINEL_HOST'), 26379)],  # discovery.endpoint DNS
    socket_timeout=0.5,
    password=os.getenv('REDIS_PASSWORD'),
    sentinel_kwargs={'password': os.getenv('REDIS_PASSWORD')}  # If sentinel auth
)

# Get master for writes
master = sentinel.master_for(
    'mymaster',  # Redis master name
    socket_timeout=5,
    password=os.getenv('REDIS_PASSWORD'),
    decode_responses=True
)

# Get slave for reads (optional)
slave = sentinel.slave_for(
    'mymaster',
    socket_timeout=5,
    password=os.getenv('REDIS_PASSWORD'),
    decode_responses=True
)

# Use master for writes
master.set('key', 'value')

# Use slave for reads (load balancing)
value = slave.get('key')
```

**Java (Jedis with Sentinel)**:
```java
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Jedis;
import java.util.HashSet;
import java.util.Set;

Set<String> sentinels = new HashSet<>();
sentinels.add(System.getenv("REDIS_SENTINEL_HOST") + ":26379");  // discovery.endpoint DNS

JedisSentinelPool pool = new JedisSentinelPool(
    "mymaster",  // Redis master name
    sentinels,
    System.getenv("REDIS_PASSWORD")
);

// Jedis automatically handles failover
try (Jedis jedis = pool.getResource()) {
    jedis.set("key", "value");
}
```

**Go (go-redis with Sentinel)**:
```go
import (
    "github.com/redis/go-redis/v9"
    "os"
)

rdb := redis.NewFailoverClient(&redis.FailoverOptions{
    MasterName:    "mymaster",  // Redis master name
    SentinelAddrs: []string{os.Getenv("REDIS_SENTINEL_HOST") + ":26379"},  // discovery.endpoint DNS
    Password:      os.Getenv("REDIS_PASSWORD"),
    DB:            0,
})

// go-redis automatically handles failover
```

**Configuration Notes**:
- `discovery.endpoint` → Sentinel service DNS (port 26379)
- Master name is typically `mymaster` (check Opstree operator config)
- Client libraries automatically discover master from Sentinels
- Failover is transparent to application (client reconnects automatically)
- Read/write splitting optional (use slaves for reads)

---

### Cluster Mode (Horizontal Scaling)
**Use Case**: Large datasets (>50GB), high write throughput, horizontal scaling

**Configuration**:
```json
{
  "deployment": {
    "mode": "cluster",
    "config": {
  "cluster": {
    "numShards": 3,
    "replicasPerShard": 1
  }
}
```
**Note**: Requires `clusterModeEnabled: true` in root schema

**Characteristics**:
- Data sharded across multiple master nodes (3-500 shards)
- Each shard has replicas for HA (1-5 replicas per shard)
- 16,384 hash slots distributed across shards
- Scales writes linearly (each shard: ~25K writes/sec)
- Automatic shard-level failover
- Client must be cluster-aware

**Architecture**:
```
Hash Slots 0-5460          Hash Slots 5461-10922      Hash Slots 10923-16383
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│   Shard 1        │       │   Shard 2        │       │   Shard 3        │
│                  │       │                  │       │                  │
│  Master (AZ-1a)  │       │  Master (AZ-1b)  │       │  Master (AZ-1c)  │
│  [writes/reads]  │       │  [writes/reads]  │       │  [writes/reads]  │
│                  │       │                  │       │                  │
│  Replica (AZ-1b) │       │  Replica (AZ-1c) │       │  Replica (AZ-1a) │
│  [reads]         │       │  [reads]         │       │  [reads]         │
└──────────────────┘       └──────────────────┘       └──────────────────┘
```

**When to Use Cluster Mode**:
- Dataset > 50GB (doesn't fit on single node)
- Write throughput > 25K ops/sec (single master bottleneck)
- Need horizontal scaling (add shards for more capacity)

**Client Requirements**:
- Use cluster-aware client libraries (redis-py-cluster, ioredis with cluster mode)
- Clients handle MOVED/ASK redirects
- Multi-key operations must be on same hash slot (use hash tags: `{user:123}:session`, `{user:123}:profile`)

**Cost**: Highest (~$500-2000+/month, scales with shards)

**IMPORTANT**: `discovery.endpoint` in root schema should point to **any Redis cluster node DNS** (port 6379)

**Application Code** (Cluster-Aware Connection):

**Node.js (ioredis with cluster mode)**:
```javascript
const Redis = require('ioredis');

const cluster = new Redis.Cluster([
  {
    host: process.env.REDIS_CLUSTER_HOST,  // discovery.endpoint DNS (any node)
    port: 6379
  }
], {
  redisOptions: {
    password: process.env.REDIS_PASSWORD
  },
  clusterRetryStrategy: (times) => Math.min(100 * times, 2000),
  enableReadyCheck: true,
  maxRedirections: 16
});

// ioredis handles MOVED/ASK redirects automatically
cluster.set('key', 'value');

// For multi-key operations, use hash tags
cluster.mget('{user:123}:session', '{user:123}:profile');  // Same hash slot
```

**Python (redis-py-cluster)**:
```python
from rediscluster import RedisCluster
import os

startup_nodes = [
    {"host": os.getenv('REDIS_CLUSTER_HOST'), "port": "6379"}  # discovery.endpoint DNS
]

rc = RedisCluster(
    startup_nodes=startup_nodes,
    password=os.getenv('REDIS_PASSWORD'),
    decode_responses=True,
    skip_full_coverage_check=True,
    max_connections_per_node=50
)

# Redis Cluster handles sharding automatically
rc.set('key', 'value')

# For multi-key operations, use hash tags
rc.mget('{user:123}:session', '{user:123}:profile')  # Same hash slot
```

**Java (Jedis with Cluster)**:
```java
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.HostAndPort;
import java.util.HashSet;
import java.util.Set;

Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort(
    System.getenv("REDIS_CLUSTER_HOST"),  // discovery.endpoint DNS
    6379
));

JedisCluster cluster = new JedisCluster(
    nodes,
    2000,  // connection timeout
    2000,  // socket timeout
    5,     // max attempts
    System.getenv("REDIS_PASSWORD")
);

// Jedis handles cluster topology and redirects
cluster.set("key", "value");

// Multi-key operations with hash tags
cluster.mget("{user:123}:session", "{user:123}:profile");
```

**Go (go-redis with Cluster)**:
```go
import (
    "github.com/redis/go-redis/v9"
    "os"
)

rdb := redis.NewClusterClient(&redis.ClusterOptions{
    Addrs:    []string{os.Getenv("REDIS_CLUSTER_HOST") + ":6379"},  // discovery.endpoint DNS
    Password: os.Getenv("REDIS_PASSWORD"),

    // Cluster options
    MaxRedirects:   16,
    ReadOnly:       false,
    RouteByLatency: true,
})

// go-redis handles cluster topology
rdb.Set(ctx, "key", "value", 0)

// Multi-key operations with hash tags
rdb.MGet(ctx, "{user:123}:session", "{user:123}:profile")
```

**Configuration Notes**:
- `discovery.endpoint` → Any Redis cluster node DNS (port 6379)
- Client discovers full cluster topology automatically
- MOVED/ASK redirects handled by client library
- Multi-key operations require hash tags: `{keyname}:suffix`
- Hash tags ensure keys map to same slot: `{user:123}:*`
- Avoid cross-slot operations (MGET across different users)

**Migration Considerations**:
When migrating from Standalone/Sentinel → Cluster:
1. **Update connection code** to use cluster client
2. **Add hash tags** to related keys for multi-key operations
3. **Test thoroughly** - cluster mode changes key distribution
4. **Transactions (MULTI/EXEC)** only work on same slot (use hash tags)
5. **Lua scripts** must access keys in same slot

## High Availability & Disaster Recovery

### Multi-AZ Distribution

Distribute Redis pods across AWS Availability Zones for resilience against AZ failures:

```json
{
  "antiAffinity": "required",
  "topologySpreadConstraints": [
    {
      "maxSkew": 1,
      "topologyKey": "topology.kubernetes.io/zone",
      "whenUnsatisfiable": "DoNotSchedule"
    }
  ]
}
```

**How it works**:
- `antiAffinity: required` prevents Redis pods on same node
- `topologySpreadConstraints` spreads pods evenly across AZs
- If AZ fails, Sentinel/Cluster promotes replica in healthy AZ

**Caveat**: EBS volumes are AZ-specific. If AZ fails:
- Pod can't restart in different AZ (volume is AZ-locked)
- Failover to replica in healthy AZ (pod with own volume)
- Original pod resumes when AZ recovers

### Pod Disruption Budgets

Protect availability during voluntary disruptions (node drains, cluster upgrades):

```json
{
  "podDisruptionBudget": {
    "enabled": true,
    "minAvailable": 2
  }
}
```

**How it works**:
- Kubernetes prevents draining nodes if it would violate `minAvailable`
- Example: With 3 Redis pods, ensures 2 are always running
- Prevents simultaneous pod evictions during maintenance

**Recommendations**:
- **Sentinel mode (1M + 2R)**: `minAvailable: 2` (master + 1 replica)
- **Cluster mode (6 nodes)**: `minAvailable: 4` (majority of nodes)

### Automated Backups

Configure S3 backups for disaster recovery:

```json
{
  "backup": {
    "enabled": true,
    "schedule": "0 2 * * *",
    "s3Bucket": "my-redis-backups",
    "s3Region": "us-east-1",
    "retention": 30
  }
}
```

**Backup Strategy**:
1. CronJob triggers Redis SAVE command (RDB snapshot)
2. Copies dump.rdb from Redis pod to S3
3. Implements retention policy (deletes old backups)

**Best Practices**:
- Schedule during low-traffic periods (2-4 AM)
- Enable S3 versioning for backup protection
- Use S3 cross-region replication for geo-redundancy
- Implement 3-2-1 rule: 3 copies, 2 media types, 1 offsite
- Test restore procedures monthly

**S3 Bucket Setup**:
```bash
# Create bucket with versioning
aws s3 mb s3://my-redis-backups --region us-east-1
aws s3api put-bucket-versioning \
  --bucket my-redis-backups \
  --versioning-configuration Status=Enabled

# Enable cross-region replication (optional)
aws s3api put-bucket-replication \
  --bucket my-redis-backups \
  --replication-configuration file://replication.json
```

### Recovery Procedures

**Scenario 1: Pod Failure**
- **Automatic**: Kubernetes restarts pod, data restored from PVC
- **Downtime**: 5-30 seconds (pod restart time)
- **Data Loss**: None (data on persistent volume)

**Scenario 2: Master Failure (Sentinel Mode)**
- **Automatic**: Sentinel promotes replica to master
- **Downtime**: 15-30 seconds (failover time)
- **Data Loss**: Minimal (0-2 seconds of writes, depends on replication lag)

**Scenario 3: AZ Failure**
- **Automatic**: Sentinel/Cluster promotes replica in healthy AZ
- **Downtime**: 15-30 seconds (failover time)
- **Data Loss**: Minimal (depends on replication lag)

**Scenario 4: Complete Cluster Failure**
- **Manual**: Restore from S3 backup
- **Procedure**:
  1. Deploy new Redis instance
  2. Copy latest backup from S3 to Redis pod
  3. Restart Redis to load dump.rdb
  4. Update application connection strings
- **Downtime**: 5-15 minutes (manual intervention)
- **Data Loss**: Since last backup (e.g., 6 hours if backup every 6 hours)

**Scenario 5: Data Corruption / Accidental Deletion**
- **Manual**: Point-in-time restore from S3 backup
- **Procedure**: Same as Scenario 4, choose specific backup
- **Data Loss**: Since chosen backup point

## Monitoring & Observability

### Prometheus Metrics

Redis exports 100+ metrics via redis-exporter on port 9121:

**Critical Metrics**:
- `redis_up`: Instance health (0=down, 1=up)
- `redis_connected_clients`: Active connections
- `redis_memory_used_bytes`: Memory usage
- `redis_memory_max_bytes`: Memory limit
- `redis_commands_processed_total`: Command throughput
- `redis_keyspace_hits_total` / `redis_keyspace_misses_total`: Cache hit ratio
- `redis_replication_lag`: Replica lag (seconds)
- `redis_cluster_state`: Cluster health (cluster mode)

### ServiceMonitor Configuration

If using Prometheus Operator, enable automatic scraping:

```json
{
  "metrics": {
    "enabled": true,
    "serviceMonitor": {
      "enabled": true,
      "interval": "30s",
      "namespace": "monitoring"
    }
  }
}
```

Prometheus Operator automatically discovers and scrapes Redis metrics.

### Grafana Dashboards

**Recommended Dashboards**:
- **Grafana Dashboard 11835**: Redis Dashboard for Prometheus Redis Exporter (most popular)
- **Grafana Dashboard 763**: Redis Dashboard (alternative)

**Import Dashboard**:
```bash
# In Grafana UI: Create → Import → Enter ID: 11835
```

**Key Panels**:
- Instance uptime and version
- Memory usage and fragmentation ratio
- Commands per second (read/write)
- Connected clients and blocked clients
- Keyspace statistics (keys, expires)
- Cache hit ratio
- Replication lag (Sentinel/Cluster)
- Cluster health and slot distribution

### CloudWatch Integration (Optional)

Export Prometheus metrics to CloudWatch for centralized AWS monitoring:

1. **Install CloudWatch Container Insights**:
   ```bash
   eksctl create iamserviceaccount \
     --name cloudwatch-agent \
     --namespace amazon-cloudwatch \
     --cluster my-cluster \
     --attach-policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy \
     --approve
   ```

2. **Deploy Fluent Bit for logs**:
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/fluent-bit/fluent-bit.yaml
   ```

### Alerting Rules

**Recommended Prometheus Alerts**:

```yaml
groups:
- name: redis-alerts
  rules:
  - alert: RedisDown
    expr: redis_up == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Redis instance {{"{{"}} $labels.instance {{"}}"}} is down"

  - alert: RedisHighMemoryUsage
    expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Redis memory usage > 80% on {{"{{"}} $labels.instance {{"}}"}}"

  - alert: RedisReplicationLag
    expr: redis_replication_lag > 5
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "Redis replica lag > 5 seconds on {{"{{"}} $labels.instance {{"}}"}}"

  - alert: RedisLowHitRatio
    expr: rate(redis_keyspace_hits_total[5m]) / (rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m])) < 0.8
    for: 10m
    labels:
      severity: info
    annotations:
      summary: "Redis cache hit ratio < 80% on {{"{{"}} $labels.instance {{"}}"}}"

  - alert: RedisHighConnections
    expr: redis_connected_clients > 9000
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Redis approaching connection limit on {{"{{"}} $labels.instance {{"}}"}}"
```

## Security

### Storage Encryption (At-Rest)

**Important**: Opstree Redis Operator does NOT have built-in support for application-level storage encryption. This is by design - Redis itself has no native at-rest encryption, and encryption should be handled at the infrastructure level.

#### Encryption Architecture

```
┌─────────────────────────────────────────────────────────┐
│            Application Layer (Redis)                    │
│  - No encryption (transparent to storage encryption)    │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│         Kubernetes Layer (PVC/PV)                       │
│  - StorageClass defines encryption policy               │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│         Infrastructure Layer (AWS EBS)                  │
│  - EBS volumes encrypted with KMS keys                  │
│  - Transparent encryption/decryption                    │
│  - Zero performance impact                              │
└─────────────────────────────────────────────────────────┘
```

#### Recommended Approach: EBS Volume Encryption

**Step 1: Create Encrypted StorageClass**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-encrypted
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"  # <-- Enable EBS encryption
  # Optional: Use customer-managed KMS key (required for HIPAA, PCI-DSS)
  kmsKeyId: "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012"
  iops: "3000"
  throughput: "125"
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
reclaimPolicy: Retain  # Optional: Keep volumes after PVC deletion for compliance
```

**Step 2: Use Encrypted StorageClass in Redis Configuration**

```json
{
  "persistence": {
    "enabled": true,
    "storageClass": "gp3-encrypted",  // <-- Reference encrypted StorageClass
    "size": "50Gi"
  }
}
```

#### Setting Up KMS Key for Encryption

**Option 1: AWS-Managed Keys (Default)**
```yaml
parameters:
  encrypted: "true"  # Uses default AWS-managed key (aws/ebs)
```

**Benefits:**
- Free (no KMS charges)
- Automatic key rotation by AWS
- No key management overhead

**Limitations:**
- Cannot meet strict compliance requirements (HIPAA, PCI-DSS)
- No control over key policies
- Cannot disable or delete key

**Option 2: Customer-Managed Keys (Compliance)**

```bash
# Create KMS key
aws kms create-key \
  --description "Redis EBS encryption key" \
  --key-usage ENCRYPT_DECRYPT

# Create alias for easier reference
aws kms create-alias \
  --alias-name alias/redis-ebs \
  --target-key-id <key-id>

# Enable automatic key rotation
aws kms enable-key-rotation \
  --key-id <key-id>

# Grant EBS permission to use key
aws kms create-grant \
  --key-id <key-id> \
  --grantee-principal ebs.amazonaws.com \
  --operations Encrypt Decrypt GenerateDataKey
```

**StorageClass with Customer-Managed Key:**
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-redis-encrypted
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
  kmsKeyId: "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012"
volumeBindingMode: WaitForFirstConsumer
```

**Benefits:**
- Full control over key lifecycle
- Custom key policies for access control
- Can disable/delete key when needed
- Meets compliance requirements (HIPAA, PCI-DSS, SOC 2)
- CloudTrail logging of all key usage

**Costs:**
- $1/month per KMS key
- $0.03 per 10,000 requests (encrypt/decrypt)
- Typical cost for Redis: $1-2/month

#### Verification

**Verify StorageClass is configured correctly:**
```bash
kubectl get storageclass gp3-encrypted -o yaml
```

**Verify PVC uses encrypted storage:**
```bash
# Get PVC name
kubectl get pvc -n <namespace>

# Get PV and volume ID
kubectl get pv <pv-name> -o jsonpath='{.spec.csi.volumeHandle}'

# Check EBS volume encryption status
aws ec2 describe-volumes --volume-ids <volume-id> \
  --query 'Volumes[0].{Encrypted:Encrypted,KmsKeyId:KmsKeyId}'

# Expected output:
# {
#   "Encrypted": true,
#   "KmsKeyId": "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012"
# }
```

#### Compliance & Security Standards

EBS encryption meets requirements for:

| Standard | Requirement | EBS Encryption Support |
|----------|-------------|----------------------|
| **HIPAA** | At-rest encryption | ✅ YES (with customer-managed KMS keys) |
| **PCI DSS** | Cardholder data encryption | ✅ YES (with customer-managed KMS keys) |
| **SOC 2** | Data encryption controls | ✅ YES |
| **GDPR** | Personal data protection | ✅ YES |
| **FedRAMP** | Federal data security | ✅ YES (with customer-managed KMS keys) |

**Additional Compliance Requirements:**
1. ✅ Enable KMS key rotation (automatic yearly rotation)
2. ✅ Implement key access policies (least privilege)
3. ✅ Enable CloudTrail logging for key usage audit
4. ✅ Document encryption policies and procedures
5. ✅ Regular security audits and key reviews

#### Complete Security Configuration

Combine encryption at-rest (EBS), in-transit (TLS), and network isolation (NetworkPolicy):

```yaml
# 1. Encrypted StorageClass
---
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-encrypted
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
  kmsKeyId: "arn:aws:kms:us-east-1:ACCOUNT:key/KEY_ID"
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true

# 2. Redis with encryption at-rest + TLS
---
apiVersion: redis.redis.opstreelabs.in/v1beta1
kind: RedisCluster
metadata:
  name: redis-secure
  namespace: redis
spec:
  # At-rest encryption (via encrypted StorageClass)
  storage:
    volumeClaimTemplate:
      spec:
        storageClassName: gp3-encrypted  # <-- Encrypted storage
        resources:
          requests:
            storage: 50Gi

  # In-transit encryption (TLS)
  TLS:
    ca: ca.crt
    cert: tls.crt
    key: tls.key
    secret:
      secretName: redis-tls-cert

  # Access control
  acl:
    secret:
      secretName: redis-acl-config

  # Pod security
  podSecurityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 1000
    seccompProfile:
      type: RuntimeDefault

  kubernetesConfig:
    image: redis:7.1
    resources:
      requests:
        cpu: 1000m
        memory: 4Gi

# 3. Network isolation (NetworkPolicy)
---
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

**Security Layers:**
1. ✅ **At-Rest Encryption**: EBS volumes encrypted with KMS
2. ✅ **In-Transit Encryption**: TLS for client-server communication
3. ✅ **Network Isolation**: NetworkPolicy restricts access
4. ✅ **Access Control**: Redis ACL for authentication
5. ✅ **Pod Security**: Non-root user, seccomp, read-only root filesystem

#### Why Not Application-Level Encryption?

**Redis does NOT support native at-rest encryption because:**

1. **Performance Impact**: Application-level encryption adds significant CPU overhead
2. **Redis Design**: Redis is designed as an in-memory cache/database, not a cryptographic system
3. **Infrastructure Abstraction**: Encryption is better handled at the block device level
4. **Zero Impact**: EBS encryption is transparent to Redis with no performance penalty

**From Redis Official Documentation:**
> "Encryption on disk should be taken care of by the infrastructure provider, and from the Redis point of view, the encryption on disk is transparent to Redis."

#### Encryption Comparison

| Approach | Performance Impact | Complexity | Cost | Recommended |
|----------|-------------------|------------|------|-------------|
| **EBS Encryption** | None (0%) | Low | Free | ✅ **YES** |
| **Application-Level** | High (20-30%) | Very High | High (dev cost) | ❌ NO |
| **File System Encryption (dm-crypt)** | Moderate (5-10%) | High | None | ❌ NO |

#### Best Practices Summary

For production Redis on EKS:

1. **Always use encrypted StorageClass** (gp3-encrypted)
2. **Use customer-managed KMS keys** for compliance (HIPAA, PCI-DSS)
3. **Enable automatic KMS key rotation** (yearly)
4. **Combine with TLS** for end-to-end encryption
5. **Enable CloudTrail** for KMS key usage auditing
6. **Document encryption policies** for compliance audits
7. **Test restore procedures** from encrypted backups
8. **Monitor KMS costs** (minimal: $1-2/month typical)

### Network Policies

Implement zero-trust networking with Kubernetes NetworkPolicy:

```json
{
  "networkPolicy": {
    "enabled": true,
    "allowedNamespaces": ["application", "backend"]
  }
}
```

**How it works**:
- Denies all ingress traffic by default
- Allows only from specified namespaces on port 6379
- Requires CNI with NetworkPolicy support (Calico, Cilium, AWS VPC CNI with network policy)

**Example NetworkPolicy Created**:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: redis-network-policy
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
    - namespaceSelector:
        matchLabels:
          name: backend
    ports:
    - protocol: TCP
      port: 6379
```

### TLS Encryption

Enable TLS for data in transit (configured in root schema):

```json
{
  "authentication": {
    "enabled": true,
    "authToken": "${REDIS_PASSWORD}"
  }
}
```

**Note**: TLS configuration (`transitEncryption`) should be added to root schema following LSP principles, as it's a universal concept across all flavors.

For now, configure via `additionalConfig`:
```json
{
  "additionalConfig": {
    "tls-port": "6379",
    "port": "0",
    "tls-cert-file": "/tls/tls.crt",
    "tls-key-file": "/tls/tls.key",
    "tls-ca-cert-file": "/tls/ca.crt"
  }
}
```

### Pod Security Standards

Pods run with restricted security context by default:

```json
{
  "securityContext": {
    "runAsNonRoot": true,
    "runAsUser": 1000,
    "fsGroup": 1000
  }
}
```

**Security Features**:
- Non-root user (UID 1000)
- Read-only root filesystem (writes only to /data volume)
- No privilege escalation
- Capabilities dropped
- Seccomp profile applied

### IAM Roles for Service Accounts (IRSA)

Use IRSA for S3 backup access without static credentials:

```bash
# Create IAM policy
aws iam create-policy \
  --policy-name RedisS3BackupPolicy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::my-redis-backups",
        "arn:aws:s3:::my-redis-backups/*"
      ]
    }]
  }'

# Create ServiceAccount with IAM role
eksctl create iamserviceaccount \
  --name redis-backup-sa \
  --namespace <namespace> \
  --cluster my-cluster \
  --attach-policy-arn arn:aws:iam::ACCOUNT_ID:policy/RedisS3BackupPolicy \
  --approve
```

Then reference in configuration:
```json
{
  "serviceAccount": "redis-backup-sa"
}
```

### Secrets Management

**Option 1: Kubernetes Secrets (Basic)**
```bash
kubectl create secret generic redis-auth \
  --from-literal=password='your-secure-password' \
  -n <namespace>
```

**Option 2: AWS Secrets Manager (Recommended)**
```bash
# Install Secrets Store CSI Driver
helm repo add secrets-store-csi-driver https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts
helm install csi-secrets-store secrets-store-csi-driver/secrets-store-csi-driver --namespace kube-system

# Install AWS Secrets Provider
kubectl apply -f https://raw.githubusercontent.com/aws/secrets-store-csi-driver-provider-aws/main/deployment/aws-provider-installer.yaml
```

Create SecretProviderClass to sync from AWS Secrets Manager to K8s Secret.

## Performance Tuning

### Resource Sizing

**Starting Points by Use Case**:

| Use Case | CPU Request | Memory Request | Storage | Cost/Month |
|----------|-------------|----------------|---------|------------|
| **Dev/Test** | 250m | 512Mi | 5Gi | ~$30 |
| **Small Prod** | 500m | 1Gi | 20Gi | ~$150 |
| **Medium Prod** | 1000m | 4Gi | 50Gi | ~$300 |
| **Large Prod** | 2000m | 8Gi | 100Gi | ~$600 |

**Memory Calculation**:
```
Kubernetes Memory Limit = Dataset Size × 1.3 (30% overhead)

Example: 5GB dataset
- Redis maxmemory: 5GB
- K8s memory limit: 6.5GB (5 × 1.3)
- K8s memory request: 6GB (slightly under limit)
```

**CPU Considerations**:
- Redis is primarily single-threaded
- 1 core ≈ 50K ops/sec (simple GET/SET commands)
- Complex commands (ZRANGE, LRANGE) use more CPU
- Background operations (RDB saves, AOF rewrites) benefit from multiple cores

### Storage Performance

**gp3 (Default - Recommended)**:
- **Baseline**: 3,000 IOPS, 125 MB/s throughput
- **Cost**: $0.08/GB-month
- **Max**: 16,000 IOPS, 1,000 MB/s (extra cost)
- **Use Case**: 99% of workloads

**io2 (High Performance)**:
- **IOPS**: Up to 64,000 IOPS per volume
- **Cost**: $0.125/GB-month + $0.065 per provisioned IOPS
- **Use Case**: Extreme write-heavy workloads (rare)

**Example**: 100GB Redis with high I/O needs
- gp3: $8/month (sufficient for most)
- io2 with 10K IOPS: $762.50/month (overkill)

**Recommendation**: gp3 for all but the most extreme workloads

### Connection Pooling

**Client Configuration Best Practices**:

**Python (redis-py)**:
```python
import redis

pool = redis.ConnectionPool(
    host='redis-master.<namespace>.svc.cluster.local',
    port=6379,
    password='your-password',
    max_connections=50,  # Pool size
    socket_keepalive=True,
    socket_connect_timeout=5,
    retry_on_timeout=True
)
redis_client = redis.Redis(connection_pool=pool)
```

**Node.js (ioredis)**:
```javascript
const Redis = require('ioredis');

const redis = new Redis({
  host: 'redis-master.<namespace>.svc.cluster.local',
  port: 6379,
  password: 'your-password',
  maxRetriesPerRequest: 3,
  enableReadyCheck: true,
  keepAlive: 30000
});
```

**Connection Limits**:
- Default Redis max clients: 10,000
- Monitor: `redis_connected_clients` metric
- Alert if > 9,000 (approaching limit)

### Redis Configuration Tuning

Use `additionalConfig` for advanced Redis settings:

```json
{
  "additionalConfig": {
    "maxmemory-policy": "allkeys-lru",
    "tcp-keepalive": "300",
    "timeout": "0",
    "tcp-backlog": "511",
    "slowlog-log-slower-than": "10000",
    "slowlog-max-len": "128"
  }
}
```

**Common Tuning Parameters**:
- `maxmemory-policy`: Eviction policy (allkeys-lru for cache, noeviction for persistence)
- `tcp-keepalive`: Keep TCP connections alive (300 seconds)
- `timeout`: Close idle connections (0 = disabled)
- `slowlog-log-slower-than`: Log slow commands (>10ms)

## Cost Optimization

### Comparison: EKS vs ElastiCache

**Scenario**: 100GB Redis with HA (1 master + 2 replicas)

| Aspect | ElastiCache | EKS (aws_k8s) | Savings |
|--------|-------------|---------------------|---------|
| **Compute** | $766/month (3 × r5.xlarge on-demand) | $420/month (3 × m5.xlarge) | $346/month |
| **Storage** | Included | $24/month (3 × 35GB gp3) | -$24/month |
| **Control Plane** | Included | $73/month (EKS) | -$73/month |
| **Load Balancer** | Included | $16/month (optional NLB) | -$16/month |
| **Backups** | $10/month | $5/month (S3) | $5/month |
| **Total (On-Demand)** | **$776/month** | **$538/month** | **$238/month (31%)** |
| **Total (1yr RI)** | **$470/month** | **$324/month** | **$146/month (31%)** |

**With Reserved Instances (1yr)**:
- ElastiCache: 40% savings
- EKS nodes: 40% savings
- **Both benefit equally from RIs**

**Break-Even Analysis**:
- Small datasets (<10GB): ElastiCache wins (managed service benefits)
- Medium datasets (10-100GB): Competitive, depends on operational expertise
- Large datasets (>100GB): EKS wins significantly ($500-1500/month savings)

### Cost Optimization Strategies

**1. Right-Size Resources**
```bash
# Monitor actual usage
kubectl top pods -n <namespace>

# Adjust resources based on observations
# Start conservative, scale up as needed
```

**2. Use Reserved Instances**
- Identify stable workloads (running > 6 months)
- Purchase 1-year EC2 reserved instances for EKS nodes
- **Savings**: 40% (m5.xlarge: $140/month → $84/month)

**3. Spot Instances for Replicas (Carefully)**
```json
{
  "nodeSelector": {
    "eks.amazonaws.com/capacityType": "SPOT"
  }
}
```
**Only for replicas**, never masters. Spot interruptions trigger failover.
**Savings**: ~70% on replica compute costs

**4. Consolidate Small Instances**
Run multiple small Redis instances on shared nodes (multi-tenancy):
```
Before: 3 apps × dedicated m5.large = $210/month
After: 3 Redis instances on shared m5.xlarge = $140/month
Savings: $70/month (33%)
```

**5. Optimize Storage**
- Use gp3 (20% cheaper than gp2, better performance)
- Right-size volumes (2-3× dataset size, not 10×)
- Implement S3 lifecycle policies (transition to Glacier after 30 days)

**6. Leverage Multi-Tenancy**
For microservices with small Redis needs:
```json
{
  "master": {
    "resources": {
      "requests": { "cpu": "250m", "memory": "512Mi" }
    }
  }
}
```
Pack 4-6 small Redis instances on single m5.large node.

## Migration Guide

### From ElastiCache to aws_k8s

**Conceptual Mapping**:

| ElastiCache | aws_k8s | Notes |
|-------------|---------------|-------|
| `cacheNodeType: cache.r5.large` | `master.resources: {cpu: 2000m, memory: 16Gi}` | Map instance type to K8s resources |
| `replicasPerNodeGroup: 2` | `deployment.config.replica.count: 2` | Same concept (for sentinel mode) |
| `numNodeGroups: 3` | `deployment.config.numShards: 3` | Cluster mode sharding |
| `multiAzEnabled: true` | `topologySpreadConstraints` | Multi-AZ via topology |
| `automaticFailoverEnabled: true` | `deployment.mode: "sentinel"` (or cluster mode) | Automatic in Sentinel/Cluster |
| `transitEncryptionEnabled: true` | `additionalConfig` (TLS) | Configure TLS manually |
| `snapshotRetentionLimit: 7` | `backup.retention: 7` | Same retention days |
| `snapshotWindow: "03:00-05:00"` | `backup.schedule: "0 3 * * *"` | Time window → cron |

**Migration Steps**:

1. **Preparation**:
   - Set up EKS cluster with Opstree operator
   - Create S3 bucket for data transfer
   - Plan maintenance window

2. **Data Export from ElastiCache**:
   ```bash
   # Create manual snapshot
   aws elasticache create-snapshot \
     --replication-group-id my-redis \
     --snapshot-name migration-snapshot

   # Export to S3
   aws elasticache copy-snapshot \
     --source-snapshot-name migration-snapshot \
     --target-snapshot-name migration-export \
     --target-bucket my-migration-bucket
   ```

3. **Deploy EKS Redis**:
   ```bash
   # Deploy with same configuration
   kubectl apply -f redis-cluster.yaml

   # Wait for pods ready
   kubectl wait --for=condition=ready pod -l app=redis -n <namespace> --timeout=300s
   ```

4. **Data Import**:
   ```bash
   # Download RDB from S3
   aws s3 cp s3://my-migration-bucket/dump.rdb /tmp/

   # Copy to Redis master pod
   kubectl cp /tmp/dump.rdb redis-master-0:/data/dump.rdb -n <namespace>

   # Restart Redis to load data
   kubectl delete pod redis-master-0 -n <namespace>
   ```

5. **Validation**:
   ```bash
   # Verify data
   kubectl exec -it redis-master-0 -n <namespace> -- redis-cli INFO keyspace

   # Test application connectivity
   # Run smoke tests
   ```

6. **Cutover**:
   - Update application connection strings to K8s Service DNS
   - Monitor for errors
   - Keep ElastiCache running for 24-48 hours as fallback
   - Decommission ElastiCache after validation

**Downtime**: 5-15 minutes (data import + application restart)

## Troubleshooting

### Common Issues

**Issue: Pods stuck in Pending**
```bash
# Check events
kubectl describe pod redis-master-0 -n <namespace>

# Common causes:
# 1. Insufficient resources
kubectl get nodes -o wide
kubectl describe nodes

# 2. Storage class not found
kubectl get storageclass

# 3. Anti-affinity too strict
# Solution: Change antiAffinity: "soft" or add more nodes
```

**Issue: Persistent volume not binding**
```bash
# Check PVC status
kubectl get pvc -n <namespace>

# Check PV provisioner
kubectl get storageclass gp3 -o yaml

# Ensure volumeBindingMode: WaitForFirstConsumer
# Ensure EBS CSI driver is running
kubectl get pods -n kube-system | grep ebs-csi
```

**Issue: Sentinel not detecting master**
```bash
# Check Sentinel logs
kubectl logs redis-sentinel-0 -n <namespace>

# Verify Sentinel configuration
kubectl exec redis-sentinel-0 -n <namespace> -- redis-cli -p 26379 SENTINEL masters

# Common fix: Ensure master Service is reachable
kubectl get svc -n <namespace>
```

**Issue: High memory usage / OOM kills**
```bash
# Check actual memory usage
kubectl top pod redis-master-0 -n <namespace>

# Check Redis memory
kubectl exec redis-master-0 -n <namespace> -- redis-cli INFO memory

# Solutions:
# 1. Increase memory limits
# 2. Enable maxmemory-policy: allkeys-lru (eviction)
# 3. Scale to cluster mode (horizontal scaling)
```

**Issue: Connection refused from application**
```bash
# Verify Service exists
kubectl get svc redis-master -n <namespace>

# Test connectivity from app namespace
kubectl run -it --rm debug --image=redis:7.1 --restart=Never -- \
  redis-cli -h redis-master.<namespace>.svc.cluster.local -a 'password' PING

# Check NetworkPolicy
kubectl get networkpolicy -n <namespace>

# Ensure application namespace is in allowedNamespaces
```

**Issue: Backup job failing**
```bash
# Check CronJob status
kubectl get cronjob -n <namespace>

# Check job logs
kubectl logs job/redis-backup-<timestamp> -n <namespace>

# Common issues:
# 1. ServiceAccount missing IRSA annotation
kubectl describe sa redis-backup-sa -n <namespace>

# 2. S3 bucket permissions
# Verify IAM policy allows s3:PutObject

# 3. S3 bucket doesn't exist
aws s3 ls s3://my-redis-backups
```

### Debug Commands

**Check Operator Status**:
```bash
kubectl get pods -n redis-operator
kubectl logs deployment/redis-operator -n redis-operator
```

**Check Redis Logs**:
```bash
kubectl logs redis-master-0 -n <namespace> -c redis
kubectl logs redis-replica-0 -n <namespace> -c redis
```

**Check Redis Metrics**:
```bash
kubectl port-forward redis-master-0 9121:9121 -n <namespace>
curl localhost:9121/metrics | grep redis_up
```

**Interactive Redis CLI**:
```bash
kubectl exec -it redis-master-0 -n <namespace> -- redis-cli -a 'password'
> INFO replication
> CLUSTER INFO  # (cluster mode)
> SENTINEL masters  # (sentinel - port 26379)
```

**Check Replication Status**:
```bash
kubectl exec redis-master-0 -n <namespace> -- redis-cli -a 'password' INFO replication
```

**Force Sentinel Failover (Testing)**:
```bash
kubectl exec redis-sentinel-0 -n <namespace> -- redis-cli -p 26379 SENTINEL failover mymaster
```

## Additional Resources

**Opstree Redis Operator**:
- Documentation: https://redis-operator.opstree.dev/
- GitHub: https://github.com/OT-CONTAINER-KIT/redis-operator
- Helm Chart: https://ot-container-kit.github.io/helm-charts/

**Redis Documentation**:
- Sentinel: https://redis.io/docs/management/sentinel/
- Cluster: https://redis.io/docs/management/scaling/
- Persistence: https://redis.io/docs/management/persistence/

**AWS EKS Best Practices**:
- Guide: https://aws.github.io/aws-eks-best-practices/
- Storage: https://docs.aws.amazon.com/eks/latest/userguide/storage.html

**Monitoring**:
- redis-exporter: https://github.com/oliver006/redis_exporter
- Grafana Dashboard 11835: https://grafana.com/grafana/dashboards/11835

**Community**:
- CNCF Redis Operator Case Study: https://www.cncf.io/blog/2024/12/17/managing-large-scale-redis-clusters-on-kubernetes-with-an-operator-kuaishous-approach/
