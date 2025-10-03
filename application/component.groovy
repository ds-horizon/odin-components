import com.dream11.Odin
import com.dream11.OdinUtil

Odin.component {
    dslVersion "v0.0.1"

    flavour {
        name "local_kubernetes"

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
}
