## Update ASG operation

Update ASG

### Update ASG operation schema

#### Properties

| Property | Type           | Required | Description                         |
|----------|----------------|----------|-------------------------------------|
| `asg`    | [object](#asg) | No       | AWS autoscaling group configuration |

#### asg

AWS autoscaling group configuration

##### Properties

| Property                              | Type                                 | Required | Description                                                                                                                        |
|---------------------------------------|--------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------|
| `capacityRebalance`                   | boolean                              | No       | Whether to enable capacityRebalance in ASG                                                                                         |
| `defaultCooldown`                     | number                               | No       | Time between scaling actions (seconds)                                                                                             |
| `defaultInstanceWarmup`               | number                               | No       | number of seconds that newly launched instances are considered as warming up, before they are counted toward the desired capacity  |
| `healthcheckGracePeriod`              | number                               | No       | ASG healthcheck grace period                                                                                                       |
| `instanceMaintenancePolicy`           | [object](#instancemaintenancepolicy) | No       | Instance maintenance policy of the ASG                                                                                             |
| `instances`                           | [object](#instances)[]               | No       | ASG instance pool configuration                                                                                                    |
| `onDemandBaseCapacity`                | number                               | No       | On demand base capacity in ASG                                                                                                     |
| `onDemandPercentageAboveBaseCapacity` | number                               | No       | On demand percentage above base capacity in ASG                                                                                    |
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


