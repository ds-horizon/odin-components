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



### Running Application
* Create a kind cluster with config
  ```yaml
  kind: Cluster
  apiVersion: kind.x-k8s.io/v1alpha4
  nodes:
  - role: control-plane
    extraMounts:
    - hostPath: /var/run/docker.sock
      containerPath: /var/run/docker.sock
    - hostPath: <host-directory>   # directory on your machine
      containerPath: /local      # path inside Kind node
  containerdConfigPatches:
  - |-
    [plugins."io.containerd.grpc.v1.cri".registry.mirrors."host.docker.internal:50000"]
      endpoint = ["http://kind-registry:5000"]%
  ```
* Start docker registry container by executing `docker run -d --restart=always -p "0.0.0.0:50000:5000" --network kind --name "kind-registry" registry:2`
* Add `host.docker.internal:50000` to docker daemon insecure registries
* Pass operation name as command line argument
* Pass following environment variables
  * `ODIN_COMPONENT_METADATA`
  * `CONFIG`

### Odin Component Metadata Structure

```json
{
    "cloudProviderDetails": {
        "account": {
            "services": [
                {
                    "name": "KIND",
                    "category": "KUBERNETES",
                    "data": {
                        "clusters": [
                            {
                                "name": "kind-kind",
				                "kubeconfig": "" // Base64 encoded config for the kubernetes cluster by kubectl config view --minify --raw | yq -r '.clusters[0].cluster.server = "https://kubernetes.default.svc.cluster.local"' | base64
                            }
                        ],
                        "environmentVariables": {}, // Default environment variables to be passed to application
                        "pullSecrets": [] // Image pull secrets for docker registry
                    }
                },
                {
                    "name": "DockerRegistry",
                    "category": "DOCKER_REGISTRY",
                    "data": {
                        "server": "http://host.docker.internal:50000",
                        "registry": "host.docker.internal:50000/odin",
		                    "insecure": true,
                        "allowPush": true
                    }
                }
            ],
            "data": {
                "homeDirectoryMountPath": "/host"
            },
            "provider": "local",
            "category": "CLOUD",
            "name": "example"
        },
        "linked_accounts": [
            {
                "services": [
                    {
                        "name": "Storage",
                        "category": "STORAGE",
                        "data": {
                            "artifacts": {
                                "repository": "" // Repository to download application artifact
                            }
                        }
                    },
                    {
                        "name": "DockerRegistry",
                        "category": "DOCKER_REGISTRY",
                        "data": {
                            "server": "", // Docker registry server
                            "registry": "",
                            "username": "",
                            "password": ""
                        }
                    }
                ],
                "name": "jfrog",
                "provider": "Jfrog",
                "category": "ARTIFACTORY",
                "data": {
                    "url": "", // Artifactory url
                    "username": "", // Artifactory username
                    "password": "" // Artifactory password
                }
            }
        ]
    },
	"name": "example-component", // Component Name
    "deploymentNamespace": "example-env" // Namespace to deploy application
}
```
