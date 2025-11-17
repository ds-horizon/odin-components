## Remove Readers operation

Removes reader instances

### Remove readers operation schema

#### Properties

| Property          | Type                         | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|-------------------|------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `readersToRemove` | [object](#readerstoremove)[] | **Yes**  | Configuration list for removing existing read replica instances from the Aurora cluster. Remove readers to reduce costs during periods of low read demand or to decommission over-provisioned capacity. Specify instance type and count to target specific readers for removal. `Impact:` Removing readers reduces read capacity and may impact query performance if remaining readers cannot handle the load. `Production:` Remove readers during low-traffic periods and monitor read latency metrics closely. Always maintain at least one reader for high availability. |

#### readersToRemove

Configuration specification identifying a group of reader instances to be removed from the cluster by instance type and count.

##### Properties

| Property        | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                   |
|-----------------|---------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `instanceCount` | integer | **Yes**  | Number of reader instances of this type to remove from the cluster. Reduce count gradually to right-size capacity without over-provisioning. `Impact:` Removing too many readers can overload remaining instances, causing increased latency and potential read failures. `Production:` Remove readers one at a time while monitoring CPU and connection metrics. Ensure at least one reader remains per AZ for availability. |
| `instanceType`  | string  | **Yes**  | The DB instance class of the reader instances to remove. Specify the exact instance type (e.g., db.r6g.large) to target specific readers. The operation will remove readers of this type. `Example:` db.r6g.large. `Production:` Verify instance type matches existing readers before removal. Consider removing smaller or analytics-focused instances first to preserve critical read capacity.                             |


