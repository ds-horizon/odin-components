import com.dream11.Odin
import com.dream11.OdinUtil

Odin.component {
    dslVersion "2.5.0"

    flavour {
        name "aws_rds"

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
          linearRetryPolicy {
              intervalSeconds 2
              count 3
            }
            tcp {
                port "3306"
            }
        }

        undeploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "bash undeploy.sh"
            out "cat state.json"
        }
    }

}