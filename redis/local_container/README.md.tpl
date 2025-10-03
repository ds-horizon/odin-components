# Redis Local Container Flavour

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

### Storage Class
Ensure your cluster has a StorageClass for persistent volumes:

**For kind/k3s:**
```bash
# Install local-path provisioner (if not already installed)
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml

# Set as default (optional)
kubectl patch storageclass local-path -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
```

**For minikube:**
```bash
# Enable hostpath storage addon
minikube addons enable storage-provisioner
```

Verify StorageClass:
```bash
kubectl get storageclass
```

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

### Storage Class Selection
Update `persistence.storageClass` in your config based on your local k8s:

- **kind/k3s**: Use `local-path`
- **minikube**: Use `hostpath` or `standard`
- **Docker Desktop**: Use `hostpath`

Example:
```json
{
  "persistence": {
    "enabled": true,
    "storageClass": "local-path",  // Adjust based on your cluster
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

{{ .Markdown 2 }}

## Differences from AWS Container Flavour

**Why can't we symlink to aws_container schema?**
See [FLAVOUR_DIFFERENCES.md](./FLAVOUR_DIFFERENCES.md) for detailed explanation of differences between local_container and aws_container flavours.

Key differences:
- **Storage**: Local StorageClass (local-path/hostpath) vs AWS EBS (gp3)
- **Load Balancing**: NodePort/port-forward vs AWS NLB
- **IAM**: No IRSA integration locally
- **Multi-AZ**: Single node/zone vs multi-AZ spreading
- **Backups**: Not supported vs Automated S3 backups

## Example Configurations

### Minimal Development Setup
```json
{
  "namespace": "redis",
  "deploymentMode": "standalone",
  "persistence": {
    "enabled": true,
    "storageClass": "local-path",
    "size": "5Gi"
  },
  "service": {
    "type": "ClusterIP"
  }
}
```

### Local HA Testing Setup
```json
{
  "namespace": "redis",
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
    "storageClass": "local-path",
    "size": "10Gi"
  }
}
```

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
