#!/usr/bin/env bash
set -euo pipefail
source ./logging.sh
setup_error_handling

{

  # Resolve RELEASE_NAME from state.json (to stay in sync with deploy.sh)
  if [[ -f state.json ]] && jq -e '.releaseName' state.json > /dev/null; then
    export RELEASE_NAME=$(jq -r '.releaseName' state.json)
  else
    echo "ERROR: No state file found for component" 1>&2
    exit 1
  fi

  export NAMESPACE={{ componentMetadata.envName }}
  export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode | default('standalone') }}

  # Function to get service endpoint
  get_redis_endpoint() {
    local service_name=$1

    # Check if service exists
    if kubectl get svc "${service_name}" -n "${NAMESPACE}" &>/dev/null; then
      # Get the cluster DNS name (without port)
      echo "${service_name}.${NAMESPACE}.svc.cluster.local"
    else
      echo "ERROR: Service ${service_name} not found in namespace ${NAMESPACE}" >&2
      exit 1
    fi
  }

  # Determine endpoint based on deployment mode
  case "${DEPLOYMENT_MODE}" in
    standalone)
      ENDPOINT=$(get_redis_endpoint "${RELEASE_NAME}-standalone")
      ;;
    sentinel)
      # For sentinel mode, return the replication service endpoint
      ENDPOINT=$(get_redis_endpoint "${RELEASE_NAME}-replication")
      ;;
    cluster)
      # For cluster mode, return the leader service for write operations
      ENDPOINT=$(get_redis_endpoint "${RELEASE_NAME}-cluster-leader")
      ;;
    *)
      echo "ERROR: Unknown deployment mode: ${DEPLOYMENT_MODE}" >&2
      exit 1
      ;;
  esac

  # IMPORTANT: print ONLY the JSON on stdout; all other logs go to stderr.
  echo "{\"primary\":\"${ENDPOINT}\", \"reader\":\"${ENDPOINT}\"}"

} 2> >(log_errors_with_timestamp)
