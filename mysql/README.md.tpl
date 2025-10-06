# Mysql component

Deploy and perform operations on your mysql component across different flavors

## Flavors
- [aws_rds](aws_rds)

{{ .Markdown 2 }}

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
