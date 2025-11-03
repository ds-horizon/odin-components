## Revert operation

Revert the version of the artifact to the previous deployed version

### Revert operation schema

#### Properties

| Property           | Type                        | Required | Description                                                            |
|--------------------|-----------------------------|----------|------------------------------------------------------------------------|
| `passiveDownscale` | [object](#passivedownscale) | No       | Whether to downscale passive ASG/LCU after successful traffic routing? |

#### passiveDownscale

Whether to downscale passive ASG/LCU after successful traffic routing?

##### Properties

| Property  | Type    | Required | Description                                     |
|-----------|---------|----------|-------------------------------------------------|
| `enabled` | boolean | **Yes**  | Enable passive ASG/LCU downscaling?             |
| `delay`   | number  | No       | Time to wait before downscaling passive ASG/LCU |


