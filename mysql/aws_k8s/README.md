## Container Flavor

Deploy your MySQL in container

### Container provisioning configuration

#### Properties

| Property          | Type               | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|-------------------|--------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `imageRegistry`   | string             | **Yes**  | Container registry URL for MySQL Docker images. Specifies where to pull MySQL container images from (e.g., docker.io, gcr.io, ECR). Use private registries for air-gapped environments, compliance requirements, or to avoid rate limits. **Required:** Must be accessible from cluster. **Production:** Use private registry with vulnerability scanning and image signing.                                                                                                                                                               |
| `imageRepository` | string             | **Yes**  | Container image repository path for MySQL. Defines the specific MySQL image to deploy (e.g., bitnami/mysql). Change to use custom images with specific configurations or patches. **Required:** Must exist in specified registry. **Production:** Use verified, security-scanned images from trusted sources.                                                                                                                                                                                                                              |
| `reader`          | [object](#reader)  | **Yes**  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `storageClass`    | string             | **Yes**  | Kubernetes storage class for MySQL persistent volumes. Determines storage performance, availability, and backup characteristics (e.g., gp3, io1, ssd). Affects database I/O performance and data durability. Choose based on IOPS requirements and backup policies. **Required:** Must exist in cluster. **Production:** Use high-performance storage (SSD/NVMe) with snapshot capabilities for production workloads.                                                                                                                      |
| `tagSuffix`       | string             | **Yes**  | MySQL Docker image version tag suffix appended to the base version. For example, if the full docker image tag is '8.0.23' and the base version '8.0' is defined in the base configuration, then this value should be '.23'. The suffix is concatenated with the base version to form the complete image tag. Changing this triggers rolling updates and may require data migrations. **Required:** Must be compatible with existing data. **Production:** Use specific version suffixes (avoid 'latest'), test upgrades in non-prod first. |
| `writer`          | [object](#writer)  | **Yes**  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `binlog`          | [object](#binlog)  | No       | MySQL binary logging configuration                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `metrics`         | [object](#metrics) | No       | MySQL metrics exporter configuration                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

#### binlog

MySQL binary logging configuration

##### Properties

| Property        | Type                     | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|-----------------|--------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`       | boolean                  | **Yes**  | Enable MySQL binary logging (binlog) for replication and point-in-time recovery. Required for primary-replica replication and disaster recovery scenarios. Enabling increases disk I/O and storage consumption. Necessary for CDC (Change Data Capture) and audit trails. **Default: `false`** (disabled for standalone). **Production:** Enable for replication setups and production databases requiring PITR capabilities.                                                              |
| `configuration` | [object](#configuration) | No       | Advanced binary log configuration parameters (key-value pairs). Customizes binlog behavior including format (ROW/STATEMENT/MIXED), retention period, file size limits. Use for fine-tuning replication performance and storage management. Misconfiguration can cause replication lag or excessive disk usage. **Optional:** Only needed for specific binlog requirements. **Production:** Configure `binlog_expire_logs_seconds` for retention and `max_binlog_size` for file management. |

##### configuration

Advanced binary log configuration parameters (key-value pairs). Customizes binlog behavior including format (ROW/STATEMENT/MIXED), retention period, file size limits. Use for fine-tuning replication performance and storage management. Misconfiguration can cause replication lag or excessive disk usage. **Optional:** Only needed for specific binlog requirements. **Production:** Configure `binlog_expire_logs_seconds` for retention and `max_binlog_size` for file management.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

#### metrics

MySQL metrics exporter configuration

##### Properties

| Property  | Type             | Required | Description                                                                                                                                                                                                                                                                                                                                                                                |
|-----------|------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled` | boolean          | **Yes**  | Enable MySQL metrics exporter for Prometheus monitoring and observability. Provides database performance metrics, connection stats, and query metrics for monitoring dashboards and alerting. Disabling reduces resource usage but eliminates metrics visibility. **Default: `true`** (recommended for production). **Production:** Keep enabled for monitoring and alerting capabilities. |
| `image`   | [object](#image) | No       |                                                                                                                                                                                                                                                                                                                                                                                            |

##### image

###### Properties

| Property     | Type   | Required | Description                                                                                                                                                                                                                                                                                                                                                              |
|--------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `registry`   | string | **Yes**  | Container image registry URL for the MySQL metrics exporter. Specifies where to pull the exporter image from (e.g., docker.io, gcr.io, or private registries). Use private registries for air-gapped environments or compliance requirements. **Default: `docker.io`** (public Docker Hub). **Production:** Use private registry for security and compliance.            |
| `repository` | string | **Yes**  | Container image repository path for the MySQL metrics exporter. Defines the specific exporter image to use for collecting MySQL metrics. Change only if using a custom or alternative exporter image. **Default: `bitnamilegacy/mysqld-exporter`** (Bitnami's MySQL exporter). **Production:** Verify image compatibility and security scanning for custom repositories. |

#### reader

##### Properties

| Property             | Type                          | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|----------------------|-------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `resources`          | [object](#resources)          | **Yes**  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `nodeSelector`       | [object](#nodeselector)       | No       | Node label selectors for MySQL writer pod placement. Constrains writer pods to nodes with matching labels (e.g., disktype=ssd, tier=database). Use to ensure pods run on nodes with appropriate hardware, zones, or compliance requirements. Empty object allows scheduling on any node. **Default: `{}`** (no constraints). **Production:** Use node selectors for dedicated DB nodes with high-performance disks and network.                                                     |
| `persistence`        | [object](#persistence)        | No       |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `replicaCount`       | integer                       | No       | Number of MySQL reader replicas for read scaling and high availability. Increase for production workloads requiring read scalability or HA. Each replica adds storage and compute costs. **Default: `0`** (standalone setup for dev/testing). **Production:** 2+ replicas recommended for HA across availability zones.                                                                                                                                                             |
| `serviceAnnotations` | [object](#serviceannotations) | No       | Kubernetes annotations to apply to the MySQL writer service. Adds custom metadata for service discovery, load balancers, or cloud provider integrations (e.g., AWS NLB annotations, service mesh configs). Annotations enable features like internal load balancers, connection draining, or custom DNS entries. **Default: `{}`** (no annotations). **Production:** Use for cloud-specific LB configurations, monitoring integrations, or service mesh sidecar injection policies. |
| `tolerations`        | [object](#tolerations)[]      | No       | Pod tolerations for MySQL writer to schedule on tainted nodes. Allows writer pods to run on nodes with specific taints (e.g., dedicated database nodes). Use to isolate database workloads on specialized hardware or availability zones. Empty array schedules on any available node. **Default: `[]`** (no tolerations). **Production:** Use dedicated node pools with taints for database isolation and predictable performance.                                                 |

##### nodeSelector

Node label selectors for MySQL writer pod placement. Constrains writer pods to nodes with matching labels (e.g., disktype=ssd, tier=database). Use to ensure pods run on nodes with appropriate hardware, zones, or compliance requirements. Empty object allows scheduling on any node. **Default: `{}`** (no constraints). **Production:** Use node selectors for dedicated DB nodes with high-performance disks and network.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

##### persistence

###### Properties

| Property | Type   | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|----------|--------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `size`   | string | **Yes**  | Persistent volume size for MySQL writer data storage. Determines available space for database files, logs, and temporary tables. Insufficient size causes write failures; over-provisioning increases costs. Size based on data growth projections plus 30-40% buffer. **Required:** Must accommodate current data + growth. **Production:** Monitor usage and plan for 12-18 months growth. PVC expansion depends on storage class support. |

##### resources

###### Properties

| Property   | Type                | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                    |
|------------|---------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `limits`   | [object](#limits)   | **Yes**  | Maximum resource limits for MySQL writer (primary) containers. Hard caps on CPU, memory, and storage to prevent resource exhaustion. Setting too low causes OOM kills or throttling; too high wastes cluster capacity. Configure based on workload sizing and peak usage patterns. **Required:** Must be >= requests. **Production:** Set 20-30% above normal peak usage with monitoring.                                      |
| `requests` | [object](#requests) | **Yes**  | Guaranteed resource requests for MySQL writer (primary) containers. Reserves minimum CPU, memory, and ephemeral storage for scheduling and QoS. Kubernetes uses this for pod placement and eviction decisions. Under-requesting causes performance issues; over-requesting reduces cluster efficiency. **Required:** Should match baseline workload needs. **Production:** Set based on load testing with 24-hour observation. |

###### limits

Maximum resource limits for MySQL writer (primary) containers. Hard caps on CPU, memory, and storage to prevent resource exhaustion. Setting too low causes OOM kills or throttling; too high wastes cluster capacity. Configure based on workload sizing and peak usage patterns. **Required:** Must be >= requests. **Production:** Set 20-30% above normal peak usage with monitoring.

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

###### requests

Guaranteed resource requests for MySQL writer (primary) containers. Reserves minimum CPU, memory, and ephemeral storage for scheduling and QoS. Kubernetes uses this for pod placement and eviction decisions. Under-requesting causes performance issues; over-requesting reduces cluster efficiency. **Required:** Should match baseline workload needs. **Production:** Set based on load testing with 24-hour observation.

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

##### serviceAnnotations

Kubernetes annotations to apply to the MySQL writer service. Adds custom metadata for service discovery, load balancers, or cloud provider integrations (e.g., AWS NLB annotations, service mesh configs). Annotations enable features like internal load balancers, connection draining, or custom DNS entries. **Default: `{}`** (no annotations). **Production:** Use for cloud-specific LB configurations, monitoring integrations, or service mesh sidecar injection policies.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

##### tolerations

###### Properties

| Property            | Type    | Required | Description                                                         |
|---------------------|---------|----------|---------------------------------------------------------------------|
| `key`               | string  | **Yes**  |                                                                     |
| `operator`          | string  | **Yes**  | Possible values are: `Equal`, `Exists`.                             |
| `effect`            | string  | No       | Possible values are: `NoSchedule`, `PreferNoSchedule`, `NoExecute`. |
| `tolerationSeconds` | integer | No       |                                                                     |
| `value`             | string  | No       |                                                                     |

#### writer

##### Properties

| Property             | Type                          | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|----------------------|-------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `resources`          | [object](#resources)          | **Yes**  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `nodeSelector`       | [object](#nodeselector)       | No       | Node label selectors for MySQL writer pod placement. Constrains writer pods to nodes with matching labels (e.g., disktype=ssd, tier=database). Use to ensure pods run on nodes with appropriate hardware, zones, or compliance requirements. Empty object allows scheduling on any node. **Default: `{}`** (no constraints). **Production:** Use node selectors for dedicated DB nodes with high-performance disks and network.                                                     |
| `persistence`        | [object](#persistence)        | No       |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `serviceAnnotations` | [object](#serviceannotations) | No       | Kubernetes annotations to apply to the MySQL writer service. Adds custom metadata for service discovery, load balancers, or cloud provider integrations (e.g., AWS NLB annotations, service mesh configs). Annotations enable features like internal load balancers, connection draining, or custom DNS entries. **Default: `{}`** (no annotations). **Production:** Use for cloud-specific LB configurations, monitoring integrations, or service mesh sidecar injection policies. |
| `tolerations`        | [object](#tolerations)[]      | No       | Pod tolerations for MySQL writer to schedule on tainted nodes. Allows writer pods to run on nodes with specific taints (e.g., dedicated database nodes). Use to isolate database workloads on specialized hardware or availability zones. Empty array schedules on any available node. **Default: `[]`** (no tolerations). **Production:** Use dedicated node pools with taints for database isolation and predictable performance.                                                 |

##### nodeSelector

Node label selectors for MySQL writer pod placement. Constrains writer pods to nodes with matching labels (e.g., disktype=ssd, tier=database). Use to ensure pods run on nodes with appropriate hardware, zones, or compliance requirements. Empty object allows scheduling on any node. **Default: `{}`** (no constraints). **Production:** Use node selectors for dedicated DB nodes with high-performance disks and network.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

##### persistence

###### Properties

| Property | Type   | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|----------|--------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `size`   | string | **Yes**  | Persistent volume size for MySQL writer data storage. Determines available space for database files, logs, and temporary tables. Insufficient size causes write failures; over-provisioning increases costs. Size based on data growth projections plus 30-40% buffer. **Required:** Must accommodate current data + growth. **Production:** Monitor usage and plan for 12-18 months growth. PVC expansion depends on storage class support. |

##### resources

###### Properties

| Property   | Type                | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                    |
|------------|---------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `limits`   | [object](#limits)   | **Yes**  | Maximum resource limits for MySQL writer (primary) containers. Hard caps on CPU, memory, and storage to prevent resource exhaustion. Setting too low causes OOM kills or throttling; too high wastes cluster capacity. Configure based on workload sizing and peak usage patterns. **Required:** Must be >= requests. **Production:** Set 20-30% above normal peak usage with monitoring.                                      |
| `requests` | [object](#requests) | **Yes**  | Guaranteed resource requests for MySQL writer (primary) containers. Reserves minimum CPU, memory, and ephemeral storage for scheduling and QoS. Kubernetes uses this for pod placement and eviction decisions. Under-requesting causes performance issues; over-requesting reduces cluster efficiency. **Required:** Should match baseline workload needs. **Production:** Set based on load testing with 24-hour observation. |

###### limits

Maximum resource limits for MySQL writer (primary) containers. Hard caps on CPU, memory, and storage to prevent resource exhaustion. Setting too low causes OOM kills or throttling; too high wastes cluster capacity. Configure based on workload sizing and peak usage patterns. **Required:** Must be >= requests. **Production:** Set 20-30% above normal peak usage with monitoring.

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

###### requests

Guaranteed resource requests for MySQL writer (primary) containers. Reserves minimum CPU, memory, and ephemeral storage for scheduling and QoS. Kubernetes uses this for pod placement and eviction decisions. Under-requesting causes performance issues; over-requesting reduces cluster efficiency. **Required:** Should match baseline workload needs. **Production:** Set based on load testing with 24-hour observation.

**Properties**

| Property           | Type   | Required | Description |
|--------------------|--------|----------|-------------|
| `cpu`              | string | **Yes**  |             |
| `ephemeralStorage` | string | **Yes**  |             |
| `memory`           | string | **Yes**  |             |

##### serviceAnnotations

Kubernetes annotations to apply to the MySQL writer service. Adds custom metadata for service discovery, load balancers, or cloud provider integrations (e.g., AWS NLB annotations, service mesh configs). Annotations enable features like internal load balancers, connection draining, or custom DNS entries. **Default: `{}`** (no annotations). **Production:** Use for cloud-specific LB configurations, monitoring integrations, or service mesh sidecar injection policies.

| Property | Type | Required | Description |
|----------|------|----------|-------------|

##### tolerations

###### Properties

| Property            | Type    | Required | Description                                                         |
|---------------------|---------|----------|---------------------------------------------------------------------|
| `key`               | string  | **Yes**  |                                                                     |
| `operator`          | string  | **Yes**  | Possible values are: `Equal`, `Exists`.                             |
| `effect`            | string  | No       | Possible values are: `NoSchedule`, `PreferNoSchedule`, `NoExecute`. |
| `tolerationSeconds` | integer | No       |                                                                     |
| `value`             | string  | No       |                                                                     |


