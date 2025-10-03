## Local Kubernetes Flavour

Deploy and perform operations on your application in local kubernetes cluster

## Operations
- [redeploy](operations/redeploy)

{{ .Markdown 3 }}

### Running Application

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `COMPONENT_METADATA`: [componentMetadata.json](../example/dev_local_kubernetes/componentMetadata.json)
  * `CONFIG`: merged json of [base_config.json](../example/dev_local_kubernetes/base_config.json) and [flavour_config.json](../example/dev_local_kubernetes/flavour_config.json). In the case of operation [operation_config.json](../example/stag_gcp_container/operation_config.json)
