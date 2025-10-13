## Add Readers operation

Adds reader instances

### Add readers operation schema

#### Properties

| Property       | Type                      | Required | Description                                                 |
|----------------|---------------------------|----------|-------------------------------------------------------------|
| `readersToAdd` | [object](#readerstoadd)[] | **Yes**  | List of configuration of New Reader DB instance to be added |

#### readersToAdd

Configuration of New Reader DB instance to be added

##### Properties

| Property        | Type    | Required | Description                            |
|-----------------|---------|----------|----------------------------------------|
| `instanceCount` | integer | **Yes**  | Number of reader instances             |
| `instanceType`  | string  | **Yes**  | e.g., db.r6g.large or db.serverless    |
| `promotionTier` | integer | No       | Promotion tier for the reader instance |


