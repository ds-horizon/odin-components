import com.dream11.Odin
import com.dream11.OdinUtil

Odin.component {
    dslVersion "v0.0.1"

    flavour {
        name "aws_rds"

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
              intervalSeconds 2
              count 3
            }
            tcp {
                port "3306"
            }
        }

        operate {
            name "add-readers"
            healthcheck false
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash execute.sh add-readers"
            out "cat state.json"
        }

        operate {
            name "remove-readers"
            healthcheck false
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash execute.sh remove-readers"
            out "cat state.json"
        }

        operate {
            name "failover"
            healthcheck true
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash execute.sh failover"
            out "cat state.json"
        }

        operate {
            name "reboot"
            healthcheck true
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash execute.sh reboot"
            out "cat state.json"
        }

        operate {
            name "update-cluster"
            healthcheck true
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "CONFIG='${getOperationConfigWithDefaults()}' bash execute.sh update-cluster"
            out "cat state.json"
        }

        undeploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }
            run "bash execute.sh undeploy"
            out "cat state.json"
        }
    }

    flavour {
        name "aws_container"
        deploy {
            String lastState = getLastState()
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }                       
            run "bash deploy.sh"
            out "cat state.json"

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
                port "3306"
            }
        }

        undeploy {
            String lastState = getLastState()    
            if (lastState != null && !lastState.isEmpty()) {
                run "echo '${lastState}' > state.json"
            }else{
                run "echo '{}' > state.json"
            }           
            run "bash undeploy.sh"
            out "cat state.json"
        }
    }   

}
