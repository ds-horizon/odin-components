# eks-component

Service Deployment Helm chart for EKS.

## Parameters

### Global parameters

| Name                 | Description                                           | Value    |
| -------------------- | ----------------------------------------------------- | -------- |
| `replicaCount`       | Number of replicas when HPA is disabled               | `1`      |
| `name`               | Short name used in resource names/labels              | `reckon` |
| `namespace`          | Target namespace used by HPA resource                 | `""`     |
| `org`                | Organization label root for helper templates          | `d11`    |
| `labels`             | Additional labels added under com.dreamsports.<org>/* | `{}`     |
| `enableServiceLinks` | Enable service links (service env vars) in pods       | `false`  |
| `annotations`        | Global annotations applied to all chart resources     | `{}`     |

### Pod Disruption Budget

| Name               | Description                                         | Value   |
| ------------------ | --------------------------------------------------- | ------- |
| `pdb.enabled`      | Enable PodDisruptionBudget                          | `false` |
| `pdb.minAvailable` | Minimum available pods during voluntary disruptions | `1`     |

### Image parameters

| Name                | Description                               | Value                  |
| ------------------- | ----------------------------------------- | ---------------------- |
| `image.registry`    | Container registry hosting the image      | `ghcr.io`              |
| `image.repository`  | Container image repository                | `stefanprodan/podinfo` |
| `image.tag`         | Container image tag                       | `5.1.4`                |
| `image.pullPolicy`  | Image pull policy                         | `IfNotPresent`         |
| `image.pullSecrets` | Image pull secrets (list of secret names) | `["regcred"]`          |

### Service parameters

| Name                   | Description                            | Value       |
| ---------------------- | -------------------------------------- | ----------- |
| `service.httpPort`     | Container HTTP port exposed by the pod | `8080`      |
| `service.externalPort` | Service port exposed for HTTP          | `80`        |

### TLS parameters

| Name             | Description                 | Value   |
| ---------------- | --------------------------- | ------- |
| `tls.enabled`    | Enable TLS service port     | `false` |
| `tls.secretName` | Name of existing TLS secret | `""`    |
| `tls.port`       | TLS service port number     | `9899`  |

### Labels

| Name     | Description             | Value |
| -------- | ----------------------- | ----- |
| `org`    | Organization label root | `d11` |
| `labels` | Additional labels (map) | `{}`  |

### Autoscaling parameters

| Name                | Description                                   | Value         |
| ------------------- | --------------------------------------------- | ------------- |
| `hpa.enabled`       | Enable HorizontalPodAutoscaler                | `false`       |
| `hpa.maxReplicas`   | Maximum number of replicas for HPA            | `10`          |
| `hpa.cpu`           | Target average CPU utilization percentage     | `80`          |
| `hpa.memory.target` | Memory target type (Utilization/AverageValue) | `Utilization` |
| `hpa.memory.value`  | Target memory value (percentage or amount)    | `80`          |
| `hpa.memory.unit`   | Unit for AverageValue target (e.g., Mi)       | `Mi`          |
| `hpa.requests`      | Average HTTP requests per second per pod      | `nil`         |

### ServiceAccount parameters

| Name                         | Description                               | Value   |
| ---------------------------- | ----------------------------------------- | ------- |
| `serviceAccount.enabled`     | Create and use a dedicated ServiceAccount | `false` |
| `serviceAccount.name`        | Custom ServiceAccount name                | `""`    |
| `serviceAccount.annotations` | ServiceAccount annotations                | `{}`    |

### HTTP routing

| Name    | Description             | Value |
| ------- | ----------------------- | ----- |
| `hosts` | Hostnames for HTTPRoute | `[]`  |

### Resources

| Name                        | Description      | Value |
| --------------------------- | ---------------- | ----- |
| `resources.limits`          | Resource limits  | `{}`  |
| `resources.requests.cpu`    | CPU requested    | `""`  |
| `resources.requests.memory` | Memory requested | `""`  |

### Scheduling

| Name           | Description                  | Value |
| -------------- | ---------------------------- | ----- |
| `nodeSelector` | Node selector for scheduling | `{}`  |
| `tolerations`  | Tolerations for scheduling   | `[]`  |
| `affinity`     | Pod affinity/anti-affinity   | `{}`  |

### Pod annotations

| Name             | Description                      | Value |
| ---------------- | -------------------------------- | ----- |
| `podAnnotations` | Extra annotations to add to pods | `{}`  |

### Lifecycle hooks

| Name                             | Description          | Value |
| -------------------------------- | -------------------- | ----- |
| `lifecycle.preStop.exec.command` | preStop hook command | `[]`  |

### Update strategy

| Name                     | Description                   | Value |
| ------------------------ | ----------------------------- | ----- |
| `flagger.maxSurge`       | Rolling update maxSurge       | `""`  |
| `flagger.maxUnavailable` | Rolling update maxUnavailable | `1`   |

### RBAC

| Name                                 | Description                                               | Value   |
| ------------------------------------ | --------------------------------------------------------- | ------- |
| `rbac.enabled`                       | Create Role/RoleBinding (requires serviceAccount.enabled) | `false` |
| `rbac.rules`                         | Additional RBAC rules                                     | `[]`    |
| `livenessProbe.enabled`              | Enable livenessProbe                                      | `true`  |
| `livenessProbe.config`               | Probe config                                              | `{}`    |
| `livenessProbe.initialDelaySeconds`  | Initial delay seconds for livenessProbe                   | `30`    |
| `livenessProbe.periodSeconds`        | Period seconds for livenessProbe                          | `30`    |
| `livenessProbe.timeoutSeconds`       | Timeout seconds for livenessProbe                         | `5`     |
| `livenessProbe.failureThreshold`     | Failure threshold for livenessProbe                       | `5`     |
| `livenessProbe.successThreshold`     | Success threshold for livenessProbe                       | `1`     |
| `readinessProbe.enabled`             | Enable readinessProbe                                     | `true`  |
| `readinessProbe.config`              | Probe config                                              | `{}`    |
| `readinessProbe.initialDelaySeconds` | Initial delay seconds for readinessProbe                  | `30`    |
| `readinessProbe.periodSeconds`       | Period seconds for readinessProbe                         | `10`    |
| `readinessProbe.timeoutSeconds`      | Timeout seconds for readinessProbe                        | `5`     |
| `readinessProbe.failureThreshold`    | Failure threshold for readinessProbe                      | `5`     |
| `readinessProbe.successThreshold`    | Success threshold for readinessProbe                      | `1`     |

The following table is generated automatically from values.yaml metadata.
