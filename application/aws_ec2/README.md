## AWS EC2 Flavor

Deploy and perform operations on your application in AWS EC2

## Operations
- [redeploy](operations/redeploy)
- [rolling-restart](operations/rolling-restart)
- [revert](operations/revert)
- [update-stack](operations/update-stack)
- [passive-downscale](operations/passive-downscale)

### AWS EC2 provisioning configuration

#### Properties

| Property       | Type                    | Required | Description                                                                           |
|----------------|-------------------------|----------|---------------------------------------------------------------------------------------|
| `asg`          | [object](#asg)          | **Yes**  | AWS autoscaling group configuration                                                   |
| `baseImages`   | [object](#baseimages)[] | **Yes**  | List of options for finding the base AMI on top of which artifact AMI will be created |
| `ebs`          | [object](#ebs)          | **Yes**  | EBS configuration                                                                     |
| `extraEnvVars` | [object](#extraenvvars) | **Yes**  | Extra environment variables to pass to application                                    |
| `loadBalancer` | [object](#loadbalancer) | **Yes**  | Load balancer configuration. Applicable only when discovery is not 'none'             |
| `stacks`       | number                  | **Yes**  | Number of stacks. Only applicable if discovery is not 'none'                          |
| `strategy`     | [object](#strategy)     | **Yes**  | Deployment strategy configuration                                                     |
| `tags`         | [object](#tags)         | **Yes**  | Custom tags to add to created resources                                               |

#### asg

AWS autoscaling group configuration

##### Properties

| Property                              | Type                                 | Required | Description                                                                                                                                           |
|---------------------------------------|--------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `capacityRebalance`                   | boolean                              | **Yes**  | Whether to enable capacityRebalance in ASG                                                                                                            |
| `desiredInstances`                    | number                               | **Yes**  | Desired number of instances. If using more than one stack specify total number of instances                                                           |
| `healthcheckGracePeriod`              | number                               | **Yes**  | ASG healthcheck grace period                                                                                                                          |
| `imdsv2`                              | string                               | **Yes**  | Whether to enable imds v2 Possible values are: `optional`, `required`.                                                                                |
| `initialCapacity`                     | number                               | **Yes**  | Initial number of instances to launch to check for application health                                                                                 |
| `instanceMaintenancePolicy`           | [object](#instancemaintenancepolicy) | **Yes**  | Instance maintenance policy of the ASG                                                                                                                |
| `instances`                           | [object](#instances)[]               | **Yes**  | ASG instance pool configuration                                                                                                                       |
| `maxInstances`                        | number                               | **Yes**  | Maximum number of instances in ASG                                                                                                                    |
| `onDemandBaseCapacity`                | number                               | **Yes**  | On demand base capacity in ASG                                                                                                                        |
| `onDemandPercentageAboveBaseCapacity` | number                               | **Yes**  | On demand percentage above base capacity in ASG                                                                                                       |
| `spotAllocationStrategy`              | string                               | **Yes**  | ASG spot allocation strategy Possible values are: `capacity-optimized`, `price-capacity-optimized`, `capacity-optimized-prioritized`, `lowest-price`. |
| `suspendProcesses`                    | string[]                             | **Yes**  | Processes to suspend in ASG                                                                                                                           |
| `terminationPolicies`                 | string[]                             | **Yes**  | ASG termination policy                                                                                                                                |
| `defaultCooldown`                     | number                               | No       | Time between scaling actions (seconds)                                                                                                                |
| `defaultInstanceWarmup`               | number                               | No       | number of seconds that newly launched instances are considered as warming up, before they are counted toward the desired capacity                     |
| `snsTopicArn`                         | string                               | No       | SNS topic where to send asg notifications                                                                                                             |

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

| Property    | Type                   | Required | Description                                                     |
|-------------|------------------------|----------|-----------------------------------------------------------------|
| `lcus`      | [object](#lcus)        | **Yes**  | Load balancer capacity units                                    |
| `listeners` | [object](#listeners)[] | **Yes**  | Listeners for load balancer                                     |
| `type`      | string                 | **Yes**  | Type of load balancer Possible values are: `alb`, `nlb`, `clb`. |

##### lcus

Load balancer capacity units

###### Properties

| Property   | Type   | Required | Description                                             |
|------------|--------|----------|---------------------------------------------------------|
| `external` | number | **Yes**  | Load balancer capacity units for external load balancer |
| `internal` | number | **Yes**  | Load balancer capacity units for internal load balancer |

##### listeners

###### Properties

| Property         | Type                    | Required | Description                                                            |
|------------------|-------------------------|----------|------------------------------------------------------------------------|
| `port`           | number                  | **Yes**  | Load balancer port                                                     |
| `protocol`       | string                  | **Yes**  | Load balancer protocol Possible values are: `HTTP`, `HTTPS`, `TCP`.    |
| `targetPort`     | number                  | **Yes**  | Instance port                                                          |
| `targetProtocol` | string                  | **Yes**  | Instance protocol Possible values are: `HTTP`, `HTTPS`, `GRPC`, `TCP`. |
| `healthchecks`   | [object](#healthchecks) | No       | Load balancer/Target group healthcheck configuration                   |

###### healthchecks

Load balancer/Target group healthcheck configuration

**Properties**

| Property             | Type   | Required | Description                                                                            |
|----------------------|--------|----------|----------------------------------------------------------------------------------------|
| `healthyThreshold`   | number | **Yes**  | Number of consecutive health check failures before declaring an EC2 instance healthy   |
| `interval`           | number | **Yes**  | Amount of time between health checks sent to EC2 instances                             |
| `path`               | string | **Yes**  | Healthcheck path                                                                       |
| `timeout`            | number | **Yes**  | Time to wait for EC2 instances to respond to health checks                             |
| `unhealthyThreshold` | number | **Yes**  | Number of consecutive health check failures before declaring an EC2 instance unhealthy |

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



### Running Application

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `ODIN_COMPONENT_METADATA`
  * `CONFIG`
