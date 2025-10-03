# Why Local Container and AWS Container Cannot Share Schema

## TL;DR

**We cannot symlink `local_container/schema.json` to `aws_container/schema.json`** because they have **environment-specific differences** in defaults, validation rules, and available features. While the schema structure is identical, the **values and descriptions** are fundamentally different.

## Background

It might seem efficient to symlink the schema files since both flavours deploy Redis on Kubernetes using the same operator. However, this would cause:
1. **Incorrect defaults** for local environments
2. **Confusing AWS-specific documentation** for local users
3. **Validation errors** for features that don't work locally
4. **Poor user experience** with mismatched expectations

## Detailed Differences

### 1. Storage Class (Critical)

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Default** | `gp3` | `standard` |
| **Description** | References AWS EBS volumes (gp3, io2), IOPS, encryption, KMS | References local storage provisioners (local-path, hostpath, standard) |
| **Validation** | Assumes EBS-specific StorageClass exists | Assumes local StorageClass exists |

**Why it matters:** Using `gp3` as default locally would fail immediately since that StorageClass doesn't exist. The entire description references AWS-specific features (encryption, IOPS, AZ topology) that are irrelevant locally.

### 2. Backup Configuration

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Automated S3 backups with CronJob | **Not supported** |
| **Storage** | S3 bucket (required when enabled) | N/A |
| **Authentication** | IAM/IRSA required | N/A |
| **Validation** | Requires `s3Bucket` field when `backup.enabled: true` | Schema has no backup section |

**Why it matters:** Backups are not relevant for local development environments. The aws_container schema **requires** `s3Bucket` field when backups are enabled, which would fail locally. Local development is ephemeral by nature.

### 3. Metrics/Monitoring Configuration

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Prometheus metrics with redis-exporter sidecar | **Not supported** |
| **ServiceMonitor** | Prometheus Operator integration available | N/A |
| **Description** | Details metrics endpoint, Prometheus scraping, Grafana dashboards | Schema has no metrics section |

**Why it matters:** Monitoring infrastructure (Prometheus, Grafana) is not typically needed for local development. Adds unnecessary complexity and resource overhead to local environments.


### 4. Service Annotations

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Description** | Details AWS Load Balancer Controller annotations (NLB, internal, cross-zone) | Generic annotations, mentions MetalLB for advanced local LB |
| **Examples** | AWS-specific: `aws-load-balancer-type: nlb` | No AWS-specific examples |
| **Relevance** | Critical for AWS LoadBalancer type | Not applicable locally |

**Why it matters:** AWS-specific annotation documentation would confuse local users who don't have AWS Load Balancer Controller.


### 5. Topology Spread Constraints

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Topology spread across zones/nodes | **Not supported** |
| **Use case** | Multi-AZ distribution, fine-grained spreading | N/A |
| **Description** | Details maxSkew, topologyKey (zone/hostname), whenUnsatisfiable | Schema has no topologySpreadConstraints section |

**Why it matters:** Topology spread constraints are for multi-AZ production deployments. Local clusters are typically single-node; even multi-node local setups don't have multiple zones. Basic pod spreading is handled by `antiAffinity` which is sufficient for local testing.


### 6. SecurityContext

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Configurable security context (UIDs, GIDs, security standards) | **Not supported** |
| **Use case** | Compliance requirements (HIPAA, PCI-DSS), organizational policies | N/A |
| **Description** | Details runAsNonRoot, runAsUser, fsGroup customization | Schema has no securityContext section |

**Why it matters:** Security context customization is for compliance and organizational policies in production. Locally, the Opstree operator already sets secure defaults (runAsNonRoot: true, runAsUser: 1000) - users don't need to override these for local testing.


### 7. Persistence Description

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Volume type** | "Uses EBS volumes in EKS, which are AZ-specific" | "Uses local storage provisioner (hostPath, local-path, standard)" |
| **Topology notes** | Pods must be in same AZ as volumes | No AZ restrictions |


### 8. NetworkPolicy

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Network isolation via NetworkPolicy | **Not supported** |
| **Use case** | Multi-tenant security, namespace isolation | N/A |
| **Description** | Details allowed namespaces, CNI requirements | Schema has no networkPolicy section |

**Why it matters:** Network policies are for production multi-tenant environments. Local single-user development doesn't need namespace-level network isolation.


### 9. PodDisruptionBudget

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | PDB for availability during disruptions | **Not supported** |
| **Use case** | Protects against node drains, cluster upgrades | N/A |
| **Description** | Details minAvailable, enabled settings | Schema has no podDisruptionBudget section |

**Why it matters:** PDBs protect against disruptions during production cluster maintenance. Local clusters don't have rolling node upgrades or maintenance windows.


### 10. ServiceAccount

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Custom ServiceAccount with IAM roles (IRSA) | **Not supported** |
| **Use case** | IAM permissions for S3, Secrets Manager, CloudWatch | N/A |
| **Description** | Details IRSA annotation, IAM role ARN | Schema has no serviceAccount section |

**Why it matters:** ServiceAccount customization is for AWS IAM integration. Local environments don't have IAM; default ServiceAccount is always sufficient.


### 11. PriorityClassName

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Pod priority for resource contention | **Not supported** |
| **Use case** | Ensures Redis preempts lower-priority workloads | N/A |
| **Description** | Details PriorityClass name, preemption behavior | Schema has no priorityClassName section |

**Why it matters:** Priority classes control pod preemption during resource contention in multi-tenant clusters. Local development typically runs only a few workloads without resource competition.


### 12. Tolerations

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Feature** | Tolerations for tainted nodes | **Not supported** |
| **Use case** | Dedicated node pools, spot instances for replicas | N/A |
| **Description** | Details toleration keys, operators, effects | Schema has no tolerations section |

**Why it matters:** Tolerations are used with taints to dedicate specific nodes for Redis (e.g., memory-optimized nodes, spot instances for replicas). Local clusters don't use taints for workload isolation or cost optimization.


### 13. Node Selector

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **Examples** | Memory-optimized r5 instances, spot vs on-demand | Generic node labels for local multi-node |
| **Production guidance** | "Dedicate memory-optimized nodes, separate master (on-demand) from replicas (spot)" | Basic node pinning for local setups |


### 14. Service Type Description

| Aspect | AWS Container | Local Container |
|--------|---------------|-----------------|
| **LoadBalancer** | "Creates AWS NLB for external access" | "Behavior depends on local k8s - may create external IP or be equivalent to NodePort" |
| **Guidance** | "Always use internal NLB annotation" | "Use NodePort for access from host machine" |


## Why Symlink Would Fail

### Immediate Technical Failures
1. **Default `storageClass: "gp3"`** → StorageClass not found error locally
2. **Backup validation requires S3 fields** → Schema validation failures when backups enabled locally
3. **Incorrect documentation** → Users following AWS-specific steps that don't apply

### User Experience Issues
1. **Confusing guidance** → References to EBS, IRSA, NLB, AZs that don't exist locally
2. **Wrong defaults** → Users would need to override many AWS-specific defaults
3. **Misleading production advice** → AWS cost/performance optimization advice irrelevant locally

## Schema Overlap Analysis

| Component | Shared Structure | Different Content |
|-----------|------------------|-------------------|
| Deployment modes | ✅ Identical | ✅ Same |
| Resource limits | ✅ Identical | ✅ Same |
| Sentinel config | ✅ Identical | ✅ Mostly same (AZ references removed) |
| Cluster config | ✅ Identical | ✅ Mostly same |
| Persistence | ✅ Identical structure | ❌ Different defaults & descriptions |
| Metrics/Monitoring | ❌ Different structure | ❌ AWS has metrics section, Local removed entirely |
| Backup | ❌ Different structure | ❌ AWS has backup section, Local removed entirely |
| TopologySpreadConstraints | ❌ Different structure | ❌ AWS has topologySpreadConstraints, Local removed entirely |
| SecurityContext | ❌ Different structure | ❌ AWS has securityContext, Local removed entirely |
| NetworkPolicy | ❌ Different structure | ❌ AWS has networkPolicy, Local removed entirely |
| PodDisruptionBudget | ❌ Different structure | ❌ AWS has podDisruptionBudget, Local removed entirely |
| ServiceAccount | ❌ Different structure | ❌ AWS has serviceAccount, Local removed entirely |
| PriorityClassName | ❌ Different structure | ❌ AWS has priorityClassName, Local removed entirely |
| Tolerations | ❌ Different structure | ❌ AWS has tolerations, Local removed entirely |
| Service | ✅ Identical structure | ❌ Different descriptions & guidance |
| Node selectors | ✅ Identical structure | ❌ Different examples & guidance |

**~50% of content is identical, but the 50% differences are critical for functionality.**
