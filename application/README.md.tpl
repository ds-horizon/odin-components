# odin-application-component

Deploy and perform operations on your application across different flavors

## Flavors
- [aws_ec2](aws_ec2)
- [gcp_container](gcp_container)
- [aws_container](aws_container)

{{ .Markdown 2 }}

## Running locally

* Update `example/*.json` accordingly
* Download DSL jar from [artifactory](https://dreamsports.jfrog.io/ui/repos/tree/General/d11-repo/com/dream11/odin-component-interface)
* Execute the following commands
```
  export PATH_TO_JAR=<path to downloaded jar>
  bash run.sh stage=<stage> operation=<operation> account_flavour=<account_flavour>
  example:
  bash run.sh stage=operate operation=redeploy account_flavour=sandbox_aws_ec2
  (account_flavour is optional default value is sandbox_aws_ec2)
```

## Contributing

* Run `bash readme-generator.sh` to auto generate README
