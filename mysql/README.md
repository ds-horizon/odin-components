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



## Running locally

* Update `example/*.json` accordingly
* Download DSL jar from [artifactory](https://dreamsports.jfrog.io/ui/repos/tree/General/d11-repo/com/dream11/odin-component-interface)
* Execute the following commands
```
  export PATH_TO_JAR=<path to downloaded jar>
  bash run.sh component=mysql stage=<stage> operation=<operation> account_flavour=<account_flavour>
  example:
  bash run.sh component=mysql stage=deploy account_flavour=stag_aws_rds
```

## Contributing

* Run `bash readme-generator.sh` to auto generate README
