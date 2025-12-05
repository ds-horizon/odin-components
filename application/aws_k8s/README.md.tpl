## GCP Container Flavor

Deploy and perform operations on your application in EKS

## Operations
- [redeploy](operations/redeploy)

{{ .Markdown 3 }}

### Running Application

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `COMPONENT_METADATA`: [componentMetadata.json](../example/internal_aws_k8s/componentMetadata.json)
  * `CONFIG`: merged json of [base_config.json](../example/internal_aws_k8s/base_config.json) and [flavour_config.json](../example/internal_aws_k8s/flavour_config.json). In the case of operation [operation_config.json](../example/internal_aws_k8s/operation_config.json)
