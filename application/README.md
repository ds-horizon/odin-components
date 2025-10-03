# odin-application-component

Deploy and perform operations on your application across different flavors

## Flavors
- [aws_ec2](aws_ec2)
- [gcp_container](gcp_container)
- [aws_container](aws_container)

## Schema valid for across all flavours

### Properties

| Property    | Type                 | Required | Description                       |
|-------------|----------------------|----------|-----------------------------------|
| `artifact`  | [object](#artifact)  | **Yes**  | Application artifact properties   |
| `discovery` | [object](#discovery) | **Yes**  | Discovery config for the artifact |

### artifact

Application artifact properties

#### Properties

| Property  | Type             | Required | Description                   |
|-----------|------------------|----------|-------------------------------|
| `hooks`   | [object](#hooks) | **Yes**  | Hooks present in the artifact |
| `name`    | string           | **Yes**  | Name of the artifact          |
| `version` | string           | **Yes**  | Version of the artifact       |

#### hooks

Hooks present in the artifact

##### Properties

| Property     | Type                  | Required | Description                                                         |
|--------------|-----------------------|----------|---------------------------------------------------------------------|
| `imageSetup` | [object](#imagesetup) | **Yes**  | Image setup hook. Execute a custom script before creating the image |
| `postDeploy` | [object](#postdeploy) | **Yes**  | Post deployment hook                                                |
| `preDeploy`  | [object](#predeploy)  | **Yes**  | Pre deployment hook                                                 |
| `start`      | [object](#start)      | **Yes**  | Start up hook                                                       |
| `stop`       | [object](#stop)       | **Yes**  | Stop application hook                                               |

##### imageSetup

Image setup hook. Execute a custom script before creating the image

###### Properties

| Property  | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| `enabled` | boolean | **Yes**  | Whether to enable this hook?         |
| `script`  | string  | **Yes**  | Script path relative to the artifact |

##### postDeploy

Post deployment hook

###### Properties

| Property      | Type    | Required | Description                               |
|---------------|---------|----------|-------------------------------------------|
| `dockerImage` | string  | **Yes**  | Docker image in which the script will run |
| `enabled`     | boolean | **Yes**  | Whether to enable this hook?              |
| `script`      | string  | **Yes**  | Script path relative to the artifact      |

##### preDeploy

Pre deployment hook

###### Properties

| Property      | Type    | Required | Description                               |
|---------------|---------|----------|-------------------------------------------|
| `dockerImage` | string  | **Yes**  | Docker image in which the script will run |
| `enabled`     | boolean | **Yes**  | Whether to enable this hook?              |
| `script`      | string  | **Yes**  | Script path relative to the artifact      |

##### start

Start up hook

###### Properties

| Property  | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| `enabled` | boolean | **Yes**  | Whether to enable this hook?         |
| `script`  | string  | **Yes**  | Script path relative to the artifact |

##### stop

Stop application hook

###### Properties

| Property  | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| `enabled` | boolean | **Yes**  | Whether to enable this hook?         |
| `script`  | string  | **Yes**  | Script path relative to the artifact |

### discovery

Discovery config for the artifact

#### Properties

| Property  | Type   | Required | Description                                        |
|-----------|--------|----------|----------------------------------------------------|
| `private` | string | No       | The private discovery endpoint for the application |
| `public`  | string | No       | The public discovery endpoint for the application  |



## Running locally

* Update `example/*.json` accordingly
* Download DSL jar from [artifactory](https://dreamsports.jfrog.io/ui/repos/tree/General/d11-repo/com/dream11/odin-component-interface)
* Execute the following commands
```
  export PATH_TO_JAR=<path to downloaded jar>
  bash run.sh stage=<stage> operation=<operation> account_flavour=<account_flavour>
  example:
  bash run.sh stage=operate operation=redeploy account_flavour=sandbox_aws_ec2
  (account_flavour is optional default value is sandbox_aws_ec2)
```

## Contributing

* Run `bash readme-generator.sh` to auto generate README
