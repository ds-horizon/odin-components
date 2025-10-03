## Redeploy operation

Redeploy application

### GCP container redeploy operation schema

#### Properties

| Property        | Type                     | Required | Description                                        |
|-----------------|--------------------------|----------|----------------------------------------------------|
| `artifact`      | [object](#artifact)      | **Yes**  | Application artifact properties                    |
| `baseImage`     | [object](#baseimage)     | No       | The base image to use for the container            |
| `extraEnvVars`  | [object](#extraenvvars)  | No       | Extra environment variables to pass to application |
| `localArtifact` | [object](#localartifact) | No       | Local artifact configuration                       |
| `ports`         | [object](#ports)[]       | No       | The ports to expose on the container               |
| `probes`        | [object](#probes)        | No       | The probes configuration for the container         |
| `replicas`      | integer                  | No       | Number of replicas to deploy                       |
| `resources`     | [object](#resources)     | No       | The resources to allocate for the container        |

#### artifact

Application artifact properties

##### Properties

| Property  | Type             | Required | Description                   |
|-----------|------------------|----------|-------------------------------|
| `version` | string           | **Yes**  | Version of the artifact       |
| `hooks`   | [object](#hooks) | No       | Hooks present in the artifact |

##### hooks

Hooks present in the artifact

###### Properties

| Property     | Type                  | Required | Description                                                         |
|--------------|-----------------------|----------|---------------------------------------------------------------------|
| `imageSetup` | [object](#imagesetup) | No       | Image setup hook. Execute a custom script before creating the image |
| `postDeploy` | [object](#postdeploy) | No       | Post deployment hook                                                |
| `preDeploy`  | [object](#predeploy)  | No       | Pre deployment hook                                                 |
| `start`      | [object](#start)      | No       | Start up hook                                                       |
| `stop`       | [object](#stop)       | No       | Stop application hook                                               |

###### imageSetup

Image setup hook. Execute a custom script before creating the image

**Properties**

| Property  | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| `enabled` | boolean | No       | Whether to enable this hook?         |
| `script`  | string  | No       | Script path relative to the artifact |

###### postDeploy

Post deployment hook

**Properties**

| Property      | Type    | Required | Description                               |
|---------------|---------|----------|-------------------------------------------|
| `dockerImage` | string  | No       | Docker image in which the script will run |
| `enabled`     | boolean | No       | Whether to enable this hook?              |
| `script`      | string  | No       | Script path relative to the artifact      |

###### preDeploy

Pre deployment hook

**Properties**

| Property      | Type    | Required | Description                               |
|---------------|---------|----------|-------------------------------------------|
| `dockerImage` | string  | No       | Docker image in which the script will run |
| `enabled`     | boolean | No       | Whether to enable this hook?              |
| `script`      | string  | No       | Script path relative to the artifact      |

###### start

Start up hook

**Properties**

| Property  | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| `enabled` | boolean | No       | Whether to enable this hook?         |
| `script`  | string  | No       | Script path relative to the artifact |

###### stop

Stop application hook

**Properties**

| Property  | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| `enabled` | boolean | No       | Whether to enable this hook?         |
| `script`  | string  | No       | Script path relative to the artifact |

#### baseImage

The base image to use for the container

##### Properties

| Property     | Type   | Required | Description                      |
|--------------|--------|----------|----------------------------------|
| `repository` | string | No       | The repository of the base image |
| `tag`        | string | No       | The tag of the base image        |

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
| `limits`   | [object](#limits)   | No       | The resource limits for the container   |
| `requests` | [object](#requests) | No       | The resource requests for the container |

##### limits

The resource limits for the container

###### Properties

| Property | Type   | Required | Description                                                 |
|----------|--------|----------|-------------------------------------------------------------|
| `cpu`    | string | No       | The amount of CPU to limit for the container, e.g. 0.5      |
| `memory` | string | No       | The amount of memory to limit for the container, e.g. 512Mi |

##### requests

The resource requests for the container

###### Properties

| Property | Type   | Required | Description                                                   |
|----------|--------|----------|---------------------------------------------------------------|
| `cpu`    | string | No       | The amount of CPU to request for the container, e.g. 0.5      |
| `memory` | string | No       | The amount of memory to request for the container, e.g. 512Mi |
