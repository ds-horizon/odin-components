## AWS EC2 Flavor

Deploy and perform operations on your application in AWS EC2

## Operations
- [redeploy](operations/redeploy)
- [rolling-restart](operations/rolling-restart)
- [revert](operations/revert)
- [update-stack](operations/update-stack)
- [passive-downscale](operations/passive-downscale)
- [scale](operations/scale)
- [update-asg](operations/update-asg)

{{ .Markdown 3 }}

### Running Application

* Pass operation name as command line argument
* Pass following environment variables
  * `ODIN_COMPONENT_METADATA`
  * `CONFIG`

### Odin Component Metadata Structure

```json
{
    "cloudProviderDetails": {
        "account": {
            "services": [
                {
                    "name": "R53",
                    "category": "DISCOVERY",
                    "data": {
                        "domains": [
                            {
                                "id": "", // Hosted Zone ID
                                "name": "example.local", // Domain Name
                                "certificateArn": "", // certificate ARN
                                "isActive": true
                            }
                        ]
                    }
                },
                {
                    "name": "VPC",
                    "category": "NETWORK",
                    "data": {
                        "lbSecurityGroups": {
                            "external": [], // External LB SGs
                            "internal": [] // Internal LB SGs
                        },
                        "lbSubnets": {
                            "private": [], // Private LB subnets
                            "public": [] // Public LB subnets
                        },
                        "ec2Subnets": {
                            "private": [] // Private EC2 subnets
                        },
                        "ec2SecurityGroups": {
                            "internal": [] // Internal EC2 SGs
                        },
                        "vpcId": "" // VPC ID
                    }
                },
                {
                    "name": "EC2",
                    "category": "VM",
                    "data": {
                        "ec2KeyName": "", // SSH Key
                        "iamInstanceProfile": "", // Role to attach to EC2
                        "ami": {
                            "sharedAccountIds": [] // AWS account IDs to search and share AMI
                        },
                        "userData": {
                            "environmentVariables": {}, // Default environment variables to be passed to application
                            "preStart": "Cg==", // Base64 encoded script to be executed before starting application
                            "postStart": "Cg==" // Base64 encoded script to be executed after starting application
                        },
                        "tags": {} // Tags to be added to all EC2 resources
                    }
                }
            ],
            "data": {
                "accountId": "", // AWS account ID
                "region": "us-east-1",
                "tags": {} // Tags to be added to all resources
            },
            "provider": "aws",
            "category": "CLOUD",
            "name": "example"
        },
        "linked_accounts": [
            {
                "services": [
                    {
                        "name": "Storage",
                        "category": "STORAGE",
                        "data": {
                            "artifacts": {
                                "repository": "" // Repository to download application artifact
                            }
                        }
                    },
                    {
                        "name": "DockerRegistry",
                        "category": "DOCKER_REGISTRY",
                        "data": {
                            "server": "", // Docker registry server
                            "registry": "",
                            "username": "",
                            "password": ""
                        }
                    }
                ],
                "name": "jfrog",
                "provider": "Jfrog",
                "category": "ARTIFACTORY",
                "data": {
                    "url": "", // Artifactory url
                    "username": "", // Artifactory username
                    "password": "" // Artifactory password
                }
            }
        ]
    },
    "name": "example-component", // Component Name
    "envName": "example-env" // Environment Name
}
```
