## Scale operation

Scale active autoscaling groups of application

### Scale operation schema

#### Properties

| Property       | Type                    | Required | Description                                                               |
|----------------|-------------------------|----------|---------------------------------------------------------------------------|
| `asg`          | [object](#asg)          | No       | AWS autoscaling group configuration                                       |
| `loadBalancer` | [object](#loadbalancer) | No       | Load balancer configuration. Applicable only when discovery is not 'none' |

#### asg

AWS autoscaling group configuration

##### Properties

| Property           | Type   | Required | Description                                                                                 |
|--------------------|--------|----------|---------------------------------------------------------------------------------------------|
| `desiredInstances` | number | No       | Desired number of instances. If using more than one stack specify total number of instances |
| `maxInstances`     | number | No       | Maximum number of instances in ASG                                                          |

#### loadBalancer

Load balancer configuration. Applicable only when discovery is not 'none'

##### Properties

| Property | Type            | Required | Description                  |
|----------|-----------------|----------|------------------------------|
| `lcus`   | [object](#lcus) | **Yes**  | Load balancer capacity units |

##### lcus

Load balancer capacity units

###### Properties

| Property   | Type   | Required | Description                                             |
|------------|--------|----------|---------------------------------------------------------|
| `external` | number | No       | Load balancer capacity units for external load balancer |
| `internal` | number | No       | Load balancer capacity units for internal load balancer |


