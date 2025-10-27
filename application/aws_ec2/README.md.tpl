## AWS EC2 Flavor

Deploy and perform operations on your application in AWS EC2

## Operations
- [redeploy](operations/redeploy)
- [rolling-restart](operations/rolling-restart)
- [revert](operations/revert)
- [update-stack](operations/update-stack)
- [passive-downscale](operations/passive-downscale)

{{ .Markdown 3 }}

### Running Application

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `ODIN_COMPONENT_METADATA`
  * `CONFIG`
