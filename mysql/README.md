# Mysql component

Deploy and perform operations on your mysql component across different flavors

## Flavors
- [aws_rds](aws_rds)

## Schema valid for across all flavours

### Properties

| Property    | Type                 | Required | Description                                             |
|-------------|----------------------|----------|---------------------------------------------------------|
| `discovery` | [object](#discovery) | **Yes**  | Discovery config for the MySQL cluster                  |
| `version`   | string               | **Yes**  | MySQL version to be created Possible values are: `8.0`. |

### discovery

Discovery config for the MySQL cluster

#### Properties

| Property | Type   | Required | Description                                   |
|----------|--------|----------|-----------------------------------------------|
| `reader` | string | **Yes**  | The private discovery endpoint for the reader |
| `writer` | string | **Yes**  | The private discovery endpoint for the writer |


