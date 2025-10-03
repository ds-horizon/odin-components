## Parameters

### Application parameters

| Name                | Description                                            | Value          |
| ------------------- | ------------------------------------------------------ | -------------- |
| `image.registry`    | Application image registry                             | `docker.io`    |
| `image.repository`  | Application image repository                           | `nginx`        |
| `image.tag`         | Application image tag (immutable tags are recommended) | `1.19.0`       |
| `image.pullPolicy`  | image pull policy                                      | `IfNotPresent` |
| `image.pullSecrets` | Application image pull secrets                         | `[]`           |
| `annotations`       | Annotations to be added on all resources               | `{}`           |
| `labels`            | Labels to be added on all resources                    | `{}`           |

### Deployment parameters

| Name                                 | Description                                              | Value           |
| ------------------------------------ | -------------------------------------------------------- | --------------- |
| `envVars`                            | Environment variables to be set on application container | `[]`            |
| `replicaCount`                       | Number of application replicas                           | `1`             |
| `nodeSelector`                       | Node labels for pod assignment                           | `{}`            |
| `tolerations`                        | Toleration for pod                                       | `[]`            |
| `strategy.type`                      | updateStrategy for Application deployment                | `RollingUpdate` |
| `podAnnotations`                     | Additional pod annotations                               | `{}`            |
| `podLabels`                          | Additional pod labels                                    | `{}`            |
| `enableServiceLinks`                 | Enable service links                                     | `false`         |
| `resources.limits`                   | The resources limits for Aerospike containers            | `{}`            |
| `resources.requests`                 | The requested resources for Aerospike containers         | `{}`            |
| `livenessProbe.enabled`              | Enable livenessProbe                                     | `true`          |
| `livenessProbe.config`               | Probe config                                             | `{}`            |
| `livenessProbe.initialDelaySeconds`  | Initial delay seconds for livenessProbe                  | `30`            |
| `livenessProbe.periodSeconds`        | Period seconds for livenessProbe                         | `30`            |
| `livenessProbe.timeoutSeconds`       | Timeout seconds for livenessProbe                        | `5`             |
| `livenessProbe.failureThreshold`     | Failure threshold for livenessProbe                      | `5`             |
| `livenessProbe.successThreshold`     | Success threshold for livenessProbe                      | `1`             |
| `readinessProbe.enabled`             | Enable readinessProbe                                    | `true`          |
| `readinessProbe.config`              | Probe config                                             | `{}`            |
| `readinessProbe.initialDelaySeconds` | Initial delay seconds for readinessProbe                 | `30`            |
| `readinessProbe.periodSeconds`       | Period seconds for readinessProbe                        | `10`            |
| `readinessProbe.timeoutSeconds`      | Timeout seconds for readinessProbe                       | `5`             |
| `readinessProbe.failureThreshold`    | Failure threshold for readinessProbe                     | `5`             |
| `readinessProbe.successThreshold`    | Success threshold for readinessProbe                     | `1`             |
| `startupProbe.enabled`               | Enable startupProbe                                      | `false`         |
| `startupProbe.config`                | Probe config                                             | `{}`            |
| `startupProbe.initialDelaySeconds`   | Initial delay seconds for startupProbe                   | `0`             |
| `startupProbe.periodSeconds`         | Period seconds for startupProbe                          | `10`            |
| `startupProbe.timeoutSeconds`        | Timeout seconds for startupProbe                         | `5`             |
| `startupProbe.failureThreshold`      | Failure threshold for startupProbe                       | `60`            |
| `startupProbe.successThreshold`      | Success threshold for startupProbe                       | `1`             |
| `ports`                              | Configure application ports                              | `[]`            |
| `lifecycle`                          | Hooks for pod lifecycle                                  | `{}`            |

### RBAC parameters

| Name                                                  | Description                                      | Value    |
|-------------------------------------------------------| ------------------------------------------------ | -------- |
| `serviceAccount.name`                                 | Name of service account for application pod      | ``       |
| `karmada.clusterAffinity`                             | Cluster affinity for propagation policy          | `{}`     |
| `karmada.replicaScheduling.replicaDivisionPreference` | Replica division preference for propagation      | `Weighted` |
| `karmada.replicaScheduling.replicaSchedulingType`     | Replica scheduling type for propagation          | `Divided` |
| `karmada.spreadConstraints`                           | to spread workload on member clusters            | `[]`     |
