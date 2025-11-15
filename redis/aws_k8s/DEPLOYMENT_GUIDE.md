# Redis AWS K8S Deployment Guide

## Summary of Changes

This deployment now supports **Opstree Redis Operator** with three deployment modes: standalone, sentinel, and cluster.

### Files Modified/Created

1. **deploy.sh** - Main deployment script
   - Auto-installs Opstree Redis Operator if not present
   - Supports all three deployment modes
   - Dynamic namespace and release name templating
   - Comprehensive status checking and reporting

2. **undeploy.sh** - Cleanup script
   - Gracefully removes all Redis resources
   - Cleans up PVCs, Services, ConfigMaps, and Secrets
   - Preserves namespace and operator for reuse
   - Detailed status reporting

3. **standalone-values.yaml** - Updated for standalone mode
   - Template placeholders for namespace and release name
   - Production-ready resource allocations (500m CPU, 1Gi memory)
   - Persistent storage with gp3 StorageClass
   - Security context configured

4. **sentinel-values.yaml** - Updated for sentinel mode
   - RedisReplication with 3 nodes (1 master + 2 replicas)
   - RedisSentinel with 3 sentinels for quorum
   - Automatic failover configuration
   - Template support for dynamic naming

5. **cluster-values.yaml** - Updated for cluster mode
   - RedisCluster with 3 masters + 3 replicas (6 total)
   - Sharding configuration
   - Node-conf persistent volumes
   - Production-ready settings

6. **QUICK_START.md** - Quick reference guide (NEW)
   - Step-by-step deployment instructions
   - Customization examples
   - Troubleshooting guide
   - Architecture diagrams

7. **DEPLOYMENT_GUIDE.md** - This document (NEW)
   - Comprehensive overview of deployment system
   - Technical details and best practices

## Deployment Modes

### Mode 1: Standalone
- **Use Case**: Development, testing, non-critical workloads
- **Components**: 1 Redis pod
- **High Availability**: No
- **Automatic Failover**: No
- **Resources**: 500m CPU, 1Gi memory, 10Gi storage
- **Cost**: ~$30-50/month

### Mode 2: Sentinel
- **Use Case**: Production HA with automatic failover
- **Components**: 
  - 3 Redis pods (1 master + 2 replicas)
  - 3 Sentinel pods
- **High Availability**: Yes
- **Automatic Failover**: Yes (15-30 seconds)
- **Resources**: 500m CPU, 1Gi memory per Redis pod
- **Cost**: ~$150-300/month

### Mode 3: Cluster
- **Use Case**: Large datasets, horizontal scaling, high throughput
- **Components**: 3 masters + 3 replicas = 6 pods
- **High Availability**: Yes (per shard)
- **Automatic Failover**: Yes (shard-level)
- **Resources**: 500m CPU, 1Gi memory per pod
- **Cost**: ~$500-800/month

## Script Features

### deploy.sh Features

1. **Automatic Operator Installation**
   - Checks if Opstree Redis Operator exists
   - Auto-installs from Helm chart if missing
   - Waits for operator to be ready

2. **Dynamic Configuration**
   - Accepts templated variables:
     - `${NAMESPACE}` - Target Kubernetes namespace
     - `${RELEASE_NAME}` - Redis release/instance name
   - Deployment mode selection via `DEPLOYMENT_MODE` variable

3. **Validation**
   - Checks for required values files
   - Validates deployment mode
   - Verifies namespace creation

4. **Status Monitoring**
   - Waits for pods to be ready (10m timeout)
   - Shows deployment status
   - Displays connection information

5. **Error Handling**
   - Comprehensive error messages
   - Graceful degradation
   - Detailed logging

### undeploy.sh Features

1. **Safe Cleanup**
   - Deletes resources by deployment mode
   - Waits for graceful termination
   - Comprehensive cleanup of all resources

2. **Resource Types Cleaned**
   - Redis CRDs (Redis, RedisReplication, RedisSentinel, RedisCluster)
   - Pods
   - PersistentVolumeClaims
   - Services
   - ConfigMaps
   - Secrets

3. **Preservation**
   - Namespace preserved for reuse
   - Operator preserved for other instances
   - Clear instructions for manual cleanup

4. **Verification**
   - Checks for remaining resources
   - Reports cleanup status
   - Warns about incomplete cleanup

## Template System

Both scripts use `envsubst` for template variable substitution:

```bash
# In deploy.sh
export NAMESPACE
export RELEASE_NAME
envsubst < "${VALUES_FILE}" > "${TEMP_MANIFEST}"
```

This allows the same YAML files to be reused for multiple deployments with different names and namespaces. The YAML files use standard shell variable syntax: `${NAMESPACE}` and `${RELEASE_NAME}`.

## Environment Variables

Required environment variables:

```bash
KUBECONFIG={{ componentMetadata.kubeConfigPath }}    # Path to kubeconfig
RELEASE_NAME={{ componentMetadata.name }}           # Redis instance name
NAMESPACE={{ componentMetadata.envName }}           # Target namespace
DEPLOYMENT_MODE={{ deployment.mode }}               # standalone|sentinel|cluster
```

These are typically provided by the Odin deployment system through template expansion.

## Resource Requirements

### Minimum Cluster Requirements

**For Standalone:**
- 1 node with 1 CPU, 2Gi memory available

**For Sentinel:**
- 3 nodes with 1 CPU, 2Gi memory each
- Recommended: Different availability zones

**For Cluster:**
- 6 nodes with 1 CPU, 2Gi memory each
- Recommended: Spread across 3 availability zones

### Storage Requirements

- **StorageClass**: `gp3` (AWS EBS)
- **Access Mode**: ReadWriteOnce
- **Sizes**:
  - Standalone: 10Gi per instance
  - Sentinel: 10Gi per pod (30Gi total)
  - Cluster: 10Gi per pod + 1Gi node-conf per pod (66Gi total)

## Operator Details

### Opstree Redis Operator

- **Repository**: https://github.com/OT-CONTAINER-KIT/redis-operator
- **Helm Chart**: ot-helm/redis-operator
- **Version**: Latest stable
- **Namespace**: redis-operator
- **CRDs**:
  - Redis (standalone)
  - RedisReplication (sentinel mode)
  - RedisSentinel (sentinel monitoring)
  - RedisCluster (cluster mode)

### Operator Installation

Automatic installation via deploy.sh:

```bash
helm repo add ot-helm https://ot-container-kit.github.io/helm-charts/
helm upgrade --install redis-operator ot-helm/redis-operator \
  --namespace redis-operator \
  --create-namespace \
  --wait \
  --timeout 5m
```

## Connection Information

After deployment, connect to Redis:

### Standalone Mode
```bash
Service: ${RELEASE_NAME}-standalone.${NAMESPACE}.svc.cluster.local:6379
```

Example:
```bash
redis-cli -h my-redis-standalone.redis.svc.cluster.local -p 6379
```

### Sentinel Mode
```bash
Sentinel Service: ${RELEASE_NAME}-sentinel.${NAMESPACE}.svc.cluster.local:26379
Redis Service: ${RELEASE_NAME}-replication.${NAMESPACE}.svc.cluster.local:6379
```

Example:
```bash
# Connect to sentinel
redis-cli -h my-redis-sentinel.redis.svc.cluster.local -p 26379

# Connect to redis (for writes)
redis-cli -h my-redis-replication.redis.svc.cluster.local -p 6379
```

### Cluster Mode
```bash
Service: ${RELEASE_NAME}-cluster.${NAMESPACE}.svc.cluster.local:6379
```

Example:
```bash
redis-cli -h my-redis-cluster.redis.svc.cluster.local -p 6379 -c
```

## Best Practices

### 1. Resource Allocation
- Start with default values (500m CPU, 1Gi memory)
- Monitor actual usage with `kubectl top pods`
- Scale up based on observed metrics
- Set requests = limits for guaranteed QoS

### 2. Storage
- Use gp3 for cost-effective performance
- Enable encryption at rest
- Size volumes at 2-3x your dataset size
- Enable volume expansion in StorageClass

### 3. High Availability
- Use Sentinel mode for production
- Deploy across multiple availability zones
- Configure pod anti-affinity
- Set appropriate PodDisruptionBudgets

### 4. Monitoring
- Enable redis-exporter (enabled by default)
- Set up Prometheus scraping
- Create Grafana dashboards
- Configure alerts for:
  - Pod down
  - High memory usage (>80%)
  - Replication lag
  - Connection limit approaching

### 5. Security
- Enable authentication (via root schema)
- Use NetworkPolicy to restrict access
- Run as non-root user (fsGroup: 1000)
- Enable TLS for production
- Encrypt persistent volumes

### 6. Backup
- Configure automated backups to S3
- Test restore procedures regularly
- Maintain backup retention policy
- Store backups in different region

## Troubleshooting

### Common Issues

1. **Pods stuck in Pending**
   - Check: `kubectl describe pod <pod-name> -n <namespace>`
   - Causes: Insufficient resources, StorageClass not found, anti-affinity too strict
   - Fix: Scale cluster, create StorageClass, or adjust affinity rules

2. **PVC not binding**
   - Check: `kubectl get pvc -n <namespace>`
   - Causes: StorageClass missing, EBS CSI driver not installed
   - Fix: Install EBS CSI driver, create gp3 StorageClass

3. **Operator not found**
   - Check: `kubectl get pods -n redis-operator`
   - Fix: deploy.sh will auto-install, or manually install operator

4. **Connection refused**
   - Check: Service exists, endpoints populated, NetworkPolicy
   - Test: Use debug pod to test connectivity
   - Fix: Verify service names, check NetworkPolicy rules

5. **Sentinel not detecting master**
   - Check: Sentinel logs, Service DNS
   - Verify: RedisReplication name matches in RedisSentinel config
   - Fix: Ensure services are created, check DNS resolution

### Debug Commands

```bash
# Check operator logs
kubectl logs -n redis-operator -l app=redis-operator

# Check Redis logs
kubectl logs <pod-name> -n <namespace> -c redis

# Check events
kubectl get events -n <namespace> --sort-by='.lastTimestamp'

# Describe pod
kubectl describe pod <pod-name> -n <namespace>

# Get CRD status
kubectl get redis,redisreplication,redissentinel,rediscluster -n <namespace> -o wide

# Test connectivity
kubectl run -it --rm debug --image=redis:7.0 --restart=Never -- \
  redis-cli -h <service-name>.<namespace>.svc.cluster.local PING
```

## Migration Path

### From Standalone to Sentinel
1. Deploy new Sentinel instance
2. Migrate data (SAVE, copy RDB, RESTORE)
3. Update application connection strings
4. Verify functionality
5. Remove standalone instance

### From Sentinel to Cluster
1. Deploy new Cluster instance
2. Use redis-cli to migrate data:
   ```bash
   redis-cli --cluster import \
     new-cluster:6379 \
     --cluster-from old-sentinel:6379 \
     --cluster-copy
   ```
3. Update application to use cluster client
4. Add hash tags to keys for multi-key operations
5. Test thoroughly
6. Remove sentinel instance

## Performance Tuning

### CPU
- Redis is mostly single-threaded
- 1 core ≈ 50K ops/sec (simple commands)
- Background operations benefit from multiple cores

### Memory
- Set maxmemory to ~80% of container limit
- Choose appropriate eviction policy
- Monitor fragmentation ratio

### Storage
- gp3 provides 3000 IOPS baseline (sufficient for most)
- Consider io2 only for extreme workloads
- Enable AOF for durability, RDB for backup

### Network
- Use ClusterIP for internal access
- LoadBalancer only for external access
- Enable NetworkPolicy for security

## Cost Optimization

1. **Right-size resources**: Start small, scale up
2. **Use Reserved Instances**: 40% savings for stable workloads
3. **Consolidate small instances**: Multi-tenancy on shared nodes
4. **Optimize storage**: Use gp3, right-size volumes
5. **Monitor and adjust**: Regular review of actual usage

## Next Steps

1. **Review** [QUICK_START.md](QUICK_START.md) for deployment instructions
2. **Customize** values files for your requirements
3. **Deploy** using deploy.sh
4. **Monitor** with Prometheus and Grafana
5. **Secure** with TLS, NetworkPolicy, and authentication
6. **Backup** to S3 with automated schedules
7. **Scale** as needed based on load

## Additional Resources

- [README.md](README.md) - Full component documentation
- [IMPLEMENTATION_MAPPING.md](IMPLEMENTATION_MAPPING.md) - CRD field mappings
- [schema.json](schema.json) - Configuration schema
- [defaults.json](defaults.json) - Default values
- [Opstree Operator Docs](https://redis-operator.opstree.dev/)
- [Redis Documentation](https://redis.io/docs/)

