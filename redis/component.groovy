import com.dream11.Odin
import com.dream11.OdinUtil

def downloadLogging = {
    download {
        provider "S3"
        uri      "s3://components-state-odin-dsl-central-prod/odin_run_files/logging.sh"
        relativeDestination "logging.sh"
    }
}

Odin.component {
    dslVersion "v0.0.2"

    flavour {
        name "aws_elasticache"

        deploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            String config = OdinUtil.mergeJsons(List.of(getBaseConfigWithDefaults(), getFlavourConfigWithDefaults()))
            run "CONFIG='${config}' bash execute.sh deploy"
            out "cat state.json"

            discovery {
                lastState = getLastState()
                String previousValidState = ""
                if (lastState != null && !lastState.isEmpty()) {
                    previousValidState = lastState
                }
                run "PREVIOUS_STATE='${previousValidState}' bash discovery.sh"
            }
        }

        healthcheck {
            linearRetryPolicy {
                count 2
                intervalSeconds 3
            }
          linearRetryPolicy {
              intervalSeconds 2
              count 3
            }
            tcp {
                port "6379"
            }
        }

        undeploy {
            downloadLogging.delegate = delegate
            downloadLogging()            
            run "bash undeploy.sh"
        }
    }   

    flavour {
        name "aws_k8s"

        deploy {
            downloadLogging.delegate = delegate
            downloadLogging()            
            run "bash deploy.sh"

            discovery {
                run "bash discovery.sh"
            }
        }
        healthcheck {
            linearRetryPolicy {
                count 2
                intervalSeconds 3
            }

            tcp {
                port "6379"
            }
        }

        undeploy {
            downloadLogging.delegate = delegate
            downloadLogging()            
            run "bash undeploy.sh"
        }
    }

    flavour {
        name "local_k8s"

        deploy {
            downloadLogging.delegate = delegate
            downloadLogging()
            run "bash deploy.sh"

            discovery {
                run "bash discovery.sh"
            }
        }

        healthcheck {
            linearRetryPolicy {
                count 2
                intervalSeconds 3
            }

            tcp {
                port "6379"
            }
        }

        undeploy {
            downloadLogging.delegate = delegate
            downloadLogging()
            run "bash undeploy.sh"
        }
    }
}
