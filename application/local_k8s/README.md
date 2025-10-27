## Local Kubernetes Flavour

Deploy and perform operations on your application in local kubernetes cluster

## Operations
- [redeploy](operations/redeploy)

### Local kubernetes flavour schema

#### Properties

| Property        | Type                     | Required | Description                                        |
|-----------------|--------------------------|----------|----------------------------------------------------|
| `baseImage`     | [object](#baseimage)     | **Yes**  | The base image to use for the container            |
| `extraEnvVars`  | [object](#extraenvvars)  | **Yes**  | Extra environment variables to pass to application |
| `localArtifact` | [object](#localartifact) | **Yes**  | Local artifact configuration                       |
| `ports`         | [object](#ports)[]       | **Yes**  | The ports to expose on the container               |
| `probes`        | [object](#probes)        | **Yes**  | The probes configuration for the container         |
| `replicas`      | integer                  | **Yes**  | Number of replicas to deploy                       |
| `resources`     | [object](#resources)     | **Yes**  | The resources to allocate for the container        |

#### baseImage

The base image to use for the container

##### Properties

| Property     | Type   | Required | Description                      |
|--------------|--------|----------|----------------------------------|
| `repository` | string | **Yes**  | The repository of the base image |
| `tag`        | string | **Yes**  | The tag of the base image        |

#### extraEnvVars

Extra environment variables to pass to application

| Property | Type | Required | Description |
|----------|------|----------|-------------|

#### localArtifact

Local artifact configuration

##### Properties

| Property  | Type    | Required | Description                       |
|-----------|---------|----------|-----------------------------------|
| `enabled` | boolean | **Yes**  | Whether local artifact is enabled |
| `path`    | string  | **Yes**  | Path to local artifact            |

#### ports

##### Properties

| Property     | Type    | Required | Description                  |
|--------------|---------|----------|------------------------------|
| `port`       | integer | **Yes**  | The port number to expose    |
| `targetPort` | integer | **Yes**  | Target port on the container |

#### probes

The probes configuration for the container

##### Properties

| Property    | Type                 | Required | Description                       |
|-------------|----------------------|----------|-----------------------------------|
| `liveness`  | [object](#liveness)  | **Yes**  | The liveness probe configuration  |
| `readiness` | [object](#readiness) | **Yes**  | The readiness probe configuration |
| `startup`   | [object](#startup)   | **Yes**  | The startup probe configuration   |

##### liveness

The liveness probe configuration

###### Properties

| Property              | Type    | Required | Description                                                              |
|-----------------------|---------|----------|--------------------------------------------------------------------------|
| `enabled`             | boolean | **Yes**  | Whether liveness probe is enabled                                        |
| `failureThreshold`    | integer | **Yes**  | The number of failed probes before the container is considered unhealthy |
| `initialDelaySeconds` | integer | **Yes**  | The initial delay before starting the probe                              |
| `intervalSeconds`     | integer | **Yes**  | The interval between probes                                              |
| `timeoutSeconds`      | integer | **Yes**  | The timeout for the probe                                                |
| `type`                | string  | **Yes**  | The type of probe to use Possible values are: `HTTP_GET`, `TCP`, `GRPC`. |

##### readiness

The readiness probe configuration

###### Properties

| Property              | Type    | Required | Description                                                                |
|-----------------------|---------|----------|----------------------------------------------------------------------------|
| `enabled`             | boolean | **Yes**  | Whether readiness probe is enabled                                         |
| `failureThreshold`    | integer | **Yes**  | The number of failed probes before the container is considered unhealthy   |
| `initialDelaySeconds` | integer | **Yes**  | The initial delay before starting the probe                                |
| `intervalSeconds`     | integer | **Yes**  | The interval between probes                                                |
| `successThreshold`    | integer | **Yes**  | The number of successful probes before the container is considered healthy |
| `timeoutSeconds`      | integer | **Yes**  | The timeout for the probe                                                  |
| `type`                | string  | **Yes**  | The type of probe to use Possible values are: `HTTP_GET`, `TCP`, `GRPC`.   |

##### startup

The startup probe configuration

###### Properties

| Property              | Type    | Required | Description                                                              |
|-----------------------|---------|----------|--------------------------------------------------------------------------|
| `enabled`             | boolean | **Yes**  | Whether startup probe is enabled                                         |
| `failureThreshold`    | integer | **Yes**  | The number of failed probes before the container is considered unhealthy |
| `initialDelaySeconds` | integer | **Yes**  | The initial delay before starting the probe                              |
| `intervalSeconds`     | integer | **Yes**  | The interval between probes                                              |
| `timeoutSeconds`      | integer | **Yes**  | The timeout for the probe                                                |
| `type`                | string  | **Yes**  | The type of probe to use Possible values are: `HTTP_GET`, `TCP`, `GRPC`. |

#### resources

The resources to allocate for the container

##### Properties

| Property   | Type                | Required | Description                             |
|------------|---------------------|----------|-----------------------------------------|
| `limits`   | [object](#limits)   | **Yes**  | The resource limits for the container   |
| `requests` | [object](#requests) | **Yes**  | The resource requests for the container |

##### limits

The resource limits for the container

###### Properties

| Property | Type   | Required | Description                                                 |
|----------|--------|----------|-------------------------------------------------------------|
| `cpu`    | string | **Yes**  | The amount of CPU to limit for the container, e.g. 0.5      |
| `memory` | string | **Yes**  | The amount of memory to limit for the container, e.g. 512Mi |

##### requests

The resource requests for the container

###### Properties

| Property | Type   | Required | Description                                                   |
|----------|--------|----------|---------------------------------------------------------------|
| `cpu`    | string | **Yes**  | The amount of CPU to request for the container, e.g. 0.5      |
| `memory` | string | **Yes**  | The amount of memory to request for the container, e.g. 512Mi |
