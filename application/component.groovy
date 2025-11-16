import com.dream11.Odin
import com.dream11.OdinUtil

Odin.component {
    dslVersion "v0.0.2"

    flavour {
        name "local_k8s"

        deploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            String config = OdinUtil.mergeJsons(List.of(getBaseConfigWithDefaults(), getFlavourConfigWithDefaults()))
            run "CONFIG='${config}' bash deploy.sh deploy"
            out "cat state.json"

            discovery {
                run "echo '{}'"
            }
        }

        healthcheck {
            script {
                filePath "./healthcheck.sh"
            }
            linearRetryPolicy {
                intervalSeconds 2
                count 3
            }
        }

        undeploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "bash operation.sh undeploy"
            out "cat state.json"
        }

        operate {
            name "redeploy"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash deploy.sh redeploy"
            out "cat state.json"
        }
    }

    flavour {
        name "aws_ec2"

        deploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            String config = OdinUtil.mergeJsons(List.of(getBaseConfigWithDefaults(), getFlavourConfigWithDefaults()))
            run "CONFIG='${config}' bash deploy.sh deploy"
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
            script {
                filePath "./healthcheck.sh"
            }
            linearRetryPolicy {
                intervalSeconds 2
                count 3
            }
        }

        undeploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "bash operation.sh undeploy"
            out "cat state.json"
        }

        operate {
            name "passive-downscale"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash operation.sh passive-downscale"
            out "cat state.json"
        }

        operate {
            name "rolling-restart"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash operation.sh rolling-restart"
            out "cat state.json"
        }

        operate {
            name "update-stack"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash operation.sh update-stack"
            out "cat state.json"
        }

        operate {
            name "revert"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash operation.sh revert"
            out "cat state.json"
        }

        operate {
            name "redeploy"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash deploy.sh redeploy"
            out "cat state.json"
        }

        operate {
            name "scale"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash operation.sh scale"
            out "cat state.json"
        }

        operate {
            name "update-asg"
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash operation.sh update-asg"
            out "cat state.json"
        }
    }
}
