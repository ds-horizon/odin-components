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
    dslVersion "v0.0.1"

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
}