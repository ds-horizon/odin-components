## aws_rds Flavour

Deploy and perform operations on your application in AWS EC2

## Operations
- [redeploy](operations/redeploy)

{{ .Markdown 3 }}

### Running Application

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `COMPONENT_METADATA`: [componentMetadata.json](../example/stag_aws_rds/componentMetadata.json)
  * `CONFIG`: merged json of [base_config.json](../example/stag_aws_rds/base_config.json) and [flavour_config.json](../example/stag_aws_rds/flavour_config.json). In the case of operation [operation_config.json](../example/stag_aws_rds/operation_config.json)
