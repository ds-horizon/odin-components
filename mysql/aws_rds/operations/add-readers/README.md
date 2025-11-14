## Add Readers operation

Adds reader instances

### Add readers operation schema

#### Properties

| Property       | Type                      | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|----------------|---------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `readersToAdd` | [object](#readerstoadd)[] | **Yes**  | Configuration list for adding new read replica instances to the Aurora cluster. Add readers to scale read capacity horizontally when experiencing high read load or to improve availability across AZs. Each reader configuration specifies instance type, count, and promotion priority. `Impact:` More readers increase read throughput but also increase costs proportionally. `Production:` Plan reader additions during low-traffic periods as instances take 5-10 minutes to provision and sync. |

#### readersToAdd

Configuration specification for a group of reader instances with identical characteristics to be added to the cluster.

##### Properties

| Property        | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|-----------------|---------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `instanceCount` | integer | **Yes**  | Number of reader instances of this type to provision. Increase count to distribute read load across more instances or improve availability. `Impact:` Each additional instance increases costs but improves read scalability and fault tolerance. `Production:` Start with 1-2 readers and scale based on read metrics. Consider at least 2 readers across different AZs for high availability.                                                                                                                        |
| `instanceType`  | string  | **Yes**  | The DB instance class that determines compute and memory capacity for the reader. Choose smaller instances (db.t3.micro) for development and larger instances (db.r6g.xlarge) for production workloads requiring high read throughput. `Example:` db.r6g.large. `Production:` Use r6g or r7g instance families for production with appropriate sizing based on read workload.                                                                                                                                          |
| `promotionTier` | integer | No       | Priority order (0-15) for promoting this reader to writer during failover, where 0 is highest priority. Set lower values for readers with instance sizes matching the writer to ensure smooth failover without capacity constraints. `Impact:` Incorrect tier settings can cause failover to undersized instances, degrading write performance. `Production:` Use tier 0-1 for production-grade readers that match writer capacity, tier 2-5 for secondary readers, and tier 10-15 for analytics or reporting readers. |


