import com.dream11.Odin
import com.dream11.OdinUtil

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
            run "bash undeploy.sh"
        }
    }   

    flavour {
        name "aws_k8s"
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
                port "6379"
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

    flavour {
        name "local_k8s"
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
                port "6379"
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
