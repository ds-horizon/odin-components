package com.dream11.application.util;

import com.dream11.application.Application;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.DiscoveryType;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtil {

  @SneakyThrows
  public DeployConfig buildDeployConfig(int numStacks, DiscoveryType discoveryType) {
    String config =
        String.format(
            """
        {
            "extraEnvVars": {
                "DISABLE_CONFIG_STORE": "true"
            },
            "stacks": %d,
            "tags": {
                "service_name": "odindemo",
                "component_name": "odindemo"
            },
            "artifact": {
                "name": "odindemo",
                "version": "1.1.7",
                "hooks": {
                    "start": {
                        "script": ".odin/start.sh",
                        "enabled": true
                    },
                    "stop": {
                        "script": ".odin/stop.sh",
                        "enabled": true
                    },
                    "preDeploy": {
                        "script": ".odin/pre-deploy.sh",
                        "enabled": true
                    },
                    "postDeploy": {
                        "script": ".odin/post-deploy.sh",
                        "enabled": true
                    },
                    "imageSetup": {
                        "script": ".odin/setup.sh",
                        "enabled": true
                    }
                }
            },
            "baseImages": [{
               "filters": {
                    "virtualization-type": "hvm",
                    "name": "odin-golden-ami-debian-java-11-*",
                    "root-device-type": "ebs",
                    "architecture": "x86_64"
                },
                "buildInstanceType": "c5.2xlarge",
                "sshUser": "centos"
            }],
            "discovery": {
                "type": "%s",
                "public": "odindemo.d11load.com",
                "private": "odindemo.dream11-load.local"
            },
            "asg": {
                "healthcheckGracePeriod": 120,
                "onDemandBaseCapacity": 0,
                "onDemandPercentageAboveBaseCapacity": 0,
                "desiredInstances": 1,
                "maxInstances": 1000,
                "initialCapacity": 1,
                "instances": [
                    {
                        "architecture":"x86_64",
                        "types": [
                            "t2.medium",
                            "c5.2xlarge",
                            "c5.xlarge"
                        ]
                    }
                ],
                "spotAllocationStrategy": "price-capacity-optimized",
                "terminationPolicies": [],
                "suspendProcesses": [],
                "snsTopicArn": "arn:aws:sns:us-east-1:640708248978:application-aws-ec2-asg"
            },
            "loadBalancer": {
                "type": "ALB",
                "listeners": [
                    {
                        "port": 80,
                        "protocol": "HTTP",
                        "targetPort": 8080,
                        "targetProtocol": "HTTP",
                        "healthchecks": {
                            "healthyThreshold": 2,
                            "unhealthyThreshold": 5,
                            "timeout": 3,
                            "interval": 6,
                            "path": "/healthcheck"
                        }
                    }
                ],
                "lcus": {
                    "internal": 1,
                    "external": 1
                }
            },
            "strategy": {
                "name": "blue-green",
                "config": {
                    "autoRouting": true,
                    "passiveDownscale": {
                        "enabled": true,
                        "delay": 1
                    },
                    "canary": {
                        "enabled": false,
                        "errorThreshold": {
                            "value": 0,
                            "metric": "ABSOLUTE"
                        },
                        "steps": {
                            "weight": 20,
                            "count": 1,
                            "duration": 180
                        }
                    }
                }
            }
        }
        """,
            numStacks, discoveryType.name());
    return Application.getObjectMapper().readValue(config, DeployConfig.class);
  }
}
