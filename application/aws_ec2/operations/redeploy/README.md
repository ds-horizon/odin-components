## Redeploy operation

Redeploy application

### Redeploy operation schema

#### Properties

| Property       | Type                    | Required | Description                                                                           |
|----------------|-------------------------|----------|---------------------------------------------------------------------------------------|
| `artifact`     | [object](#artifact)     | **Yes**  | Application artifact properties                                                       |
| `asg`          | [object](#asg)          | No       | AWS autoscaling group configuration                                                   |
| `baseImages`   | [object](#baseimages)[] | No       | List of options for finding the base AMI on top of which artifact AMI will be created |
| `ebs`          | [object](#ebs)          | No       | EBS configuration                                                                     |
| `extraEnvVars` | [object](#extraenvvars) | No       | Extra environment variables to pass to application                                    |
| `loadBalancer` | [object](#loadbalancer) | No       | Load balancer configuration. Applicable only when discovery is not 'none'             |
| `strategy`     | [object](#strategy)     | No       | Deployment strategy configuration                                                     |
| `tags`         | [object](#tags)         | No       | Custom tags to add to created resources                                               |

#### artifact

Application artifact properties

##### Properties

| Property  | Type             | Required | Description                   |
|-----------|------------------|----------|-------------------------------|
| `version` | string           | **Yes**  | Version of the artifact       |
| `hooks`   | [object](#hooks) | No       | Hooks present in the artifact |
| `name`    | string           | No       | Name of the artifact          |

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

#### asg

AWS autoscaling group configuration

##### Properties

| Property                              | Type                                 | Required | Description                                                                                                                        |
|---------------------------------------|--------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------|
| `capacityRebalance`                   | boolean                              | No       | Whether to enable capacityRebalance in ASG                                                                                         |
| `defaultCooldown`                     | number                               | No       | Time between scaling actions (seconds)                                                                                             |
| `defaultInstanceWarmup`               | number                               | No       | number of seconds that newly launched instances are considered as warming up, before they are counted toward the desired capacity  |
| `desiredInstances`                    | number                               | No       | Desired number of instances. If using more than one stack specify total number of instances                                        |
| `healthcheckGracePeriod`              | number                               | No       | ASG healthcheck grace period                                                                                                       |
| `imdsv2`                              | string                               | No       | Whether to enable imds v2 Possible values are: `optional`, `required`.                                                             |
| `initialCapacity`                     | number                               | No       | Initial number of instances to launch to check for application health                                                              |
| `instanceMaintenancePolicy`           | [object](#instancemaintenancepolicy) | No       | Instance maintenance policy of the ASG                                                                                             |
| `instances`                           | [object](#instances)[]               | No       | ASG instance pool configuration                                                                                                    |
| `maxInstances`                        | number                               | No       | Maximum number of instances in ASG                                                                                                 |
| `onDemandBaseCapacity`                | number                               | No       | On demand base capacity in ASG                                                                                                     |
| `onDemandPercentageAboveBaseCapacity` | number                               | No       | On demand percentage above base capacity in ASG                                                                                    |
| `snsTopicArn`                         | string                               | No       | SNS topic where to send asg notifications                                                                                          |
| `spotAllocationStrategy`              | string                               | No       | ASG spot allocation strategy Possible values are: `capacity-optimized`, `price-capacity-optimized`, `diversified`, `lowest-price`. |
| `suspendProcesses`                    | string[]                             | No       | Processes to suspend in ASG                                                                                                        |
| `terminationPolicies`                 | string[]                             | No       | ASG termination policy                                                                                                             |

##### instanceMaintenancePolicy

Instance maintenance policy of the ASG

###### Properties

| Property               | Type   | Required | Description                         |
|------------------------|--------|----------|-------------------------------------|
| `maxHealthyPercentage` | number | **Yes**  | Maximum healthy instance percentage |
| `minHealthyPercentage` | number | **Yes**  | Minimum healthy instance percentage |

##### instances

Instance types for architecture

###### Properties

| Property       | Type     | Required | Description                   |
|----------------|----------|----------|-------------------------------|
| `architecture` | string   | **Yes**  | Architecture of the instances |
| `types`        | string[] | **Yes**  | Instances types               |

#### baseImages

##### Properties

| Property            | Type               | Required | Description                                                                                                                |
|---------------------|--------------------|----------|----------------------------------------------------------------------------------------------------------------------------|
| `buildInstanceType` | string             | **Yes**  | Instance type to launch when for AMI creation                                                                              |
| `filters`           | [object](#filters) | **Yes**  | Filters to search base AMI. Allowed values: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeImages.html |
| `sshUser`           | string             | **Yes**  | SSH username for base AMI                                                                                                  |

##### filters

Filters to search base AMI. Allowed values: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeImages.html

###### Properties

| Property       | Type   | Required | Description             |
|----------------|--------|----------|-------------------------|
| `architecture` | string | **Yes**  | Architecture of the AMI |

#### ebs

EBS configuration

##### Properties

| Property | Type   | Required | Description                |
|----------|--------|----------|----------------------------|
| `size`   | number | **Yes**  | Desired size of ebs volume |

#### extraEnvVars

Extra environment variables to pass to application

| Property | Type | Required | Description |
|----------|------|----------|-------------|

#### loadBalancer

Load balancer configuration. Applicable only when discovery is not 'none'

##### Properties

| Property | Type            | Required | Description                  |
|----------|-----------------|----------|------------------------------|
| `lcus`   | [object](#lcus) | No       | Load balancer capacity units |

##### lcus

Load balancer capacity units

###### Properties

| Property   | Type   | Required | Description                                                                     |
|------------|--------|----------|---------------------------------------------------------------------------------|
| `external` | number | No       | Load balancer capacity units for external load balancer, not applicable for clb |
| `internal` | number | No       | Load balancer capacity units for internal load balancer, not applicable for clb |

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

Custom tags to add to created resources

| Property | Type | Required | Description |
|----------|------|----------|-------------|


