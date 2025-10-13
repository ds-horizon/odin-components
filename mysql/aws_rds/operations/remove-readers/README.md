## Remove Readers operation

Removes reader instances

### Remove readers operation schema

#### Properties

| Property          | Type                         | Required | Description                                                |
|-------------------|------------------------------|----------|------------------------------------------------------------|
| `readersToRemove` | [object](#readerstoremove)[] | **Yes**  | List of configuration of Reader DB instances to be removed |

#### readersToRemove

Configuration of Reader DB instance to be removed

##### Properties

| Property        | Type    | Required | Description                         |
|-----------------|---------|----------|-------------------------------------|
| `instanceCount` | integer | **Yes**  | Number of reader instances          |
| `instanceType`  | string  | **Yes**  | e.g., db.r6g.large or db.serverless |


