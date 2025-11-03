## Local Kubernetes Flavour

Deploy and perform operations on your application in local kubernetes cluster

## Operations
- [redeploy](operations/redeploy)

{{ .Markdown 3 }}

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
