## aws_rds Flavour

Deploy and perform operations on your application in AWS RDS

## Operations
- [add-readers](operations/add-readers)
- [remove-readers](operation/remove-readers)

{{ .Markdown 3 }}

### Running Mysql

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `COMPONENT_METADATA`: [componentMetadata.json](../example/stag_aws_rds/componentMetadata.json)
  * `CONFIG`: merged json of [base_config.json](../example/stag_aws_rds/base_config.json) and [flavour_config.json](../example/stag_aws_rds/flavour_config.json). In the case of operation [operation_config.json](../example/stag_aws_rds/operation_config.json)
