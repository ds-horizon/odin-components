## Redeploy operation

Redeploy application

### AWS container redeploy operation schema

#### Properties

| Property       | Type                     | Required | Description                                        |
|----------------|--------------------------|----------|----------------------------------------------------|
| `artifact`     | [object](#artifact)      | **Yes**  | Application artifact properties                    |
| `baseImage`    | [object](#baseimage)     | No       | The base image to use for the container            |
| `extraEnvVars` | [object](#extraenvvars)  | No       | Extra environment variables to pass to application |
| `nodeSelector` | [object](#nodeselector)  | No       | Node selector for the application pods             |
| `replicas`     | integer                  | No       | Number of replicas to deploy                       |
| `resources`    | [object](#resources)     | No       | The resources to allocate for the container        |
| `strategy`     | [object](#strategy)      | No       | Deployment strategy configuration                  |
| `tags`         | [object](#tags)          | No       | Custom labels to add to created resources          |
| `tolerations`  | [object](#tolerations)[] | No       | Tolerations for the application pods               |

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

#### nodeSelector

Node selector for the application pods

| Property | Type | Required | Description |
|----------|------|----------|-------------|

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

#### strategy

Deployment strategy configuration

##### Properties

| Property | Type              | Required | Description                                                    |
|----------|-------------------|----------|----------------------------------------------------------------|
| `config` | [object](#config) | **Yes**  | Deployment strategy configuration                              |
| `name`   | string            | **Yes**  | Name of deployment strategy Possible values are: `blue-green`. |

##### config

Deployment strategy configuration

| Property | Type | Required | Description |
|----------|------|----------|-------------|

#### tags

Custom labels to add to created resources

| Property | Type | Required | Description |
|----------|------|----------|-------------|

#### tolerations

##### Properties

| Property   | Type   | Required | Description                                                                                                              |
|------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------|
| `effect`   | string | No       | The effect of the toleration. Defaults to NoSchedule Possible values are: `NoSchedule`, `PreferNoSchedule`, `NoExecute`. |
| `key`      | string | No       | The key of the toleration                                                                                                |
| `operator` | string | No       | The operator of the toleration. Defaults to Equal Possible values are: `Equal`, `Exists`.                                |
| `value`    | string | No       | The value of the toleration                                                                                              |


