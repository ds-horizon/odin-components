# odin-application-component

Deploy and perform operations on your application across different flavors

## Flavors
- [local_k8s](local_k8s)
- [aws_ec2](aws_ec2)
{{ .Markdown 2 }}

### Running Application

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `ODIN_COMPONENT_METADATA`
  * `CONFIG`
