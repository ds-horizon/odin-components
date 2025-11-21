## GCP Container Flavor

Deploy and perform operations on your application in EKS

## Operations
- [redeploy](operations/redeploy)

{{ .Markdown 3 }}

### Running Application

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `COMPONENT_METADATA`: [componentMetadata.json](../example/internal_aws_container/componentMetadata.json)
  * `CONFIG`: merged json of [base_config.json](../example/internal_aws_container/base_config.json) and [flavour_config.json](../example/internal_aws_container/flavour_config.json). In the case of operation [operation_config.json](../example/internal_aws_container/operation_config.json)
