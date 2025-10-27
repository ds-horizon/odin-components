## Rolling restart operation

Restart the application servers in batches

### Passive downscale operation schema

#### Properties

| Property                   | Type    | Required | Description                                                             |
|----------------------------|---------|----------|-------------------------------------------------------------------------|
| `batchSizePercentage`      | integer | **Yes**  | Percentage of instances where this operation will run in parallel       |
| `errorTolerancePercentage` | integer | **Yes**  | Allowed percentage of instances that fail during this operation         |
| `mode`                     | string  | **Yes**  | The mode of the operation Possible values are: `FORCEKILL`, `GRACEFUL`. |


