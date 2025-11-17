#!/usr/bin/env bash
set -euo pipefail

# Environment variables from component metadata
# These are set by the deployment framework or can be provided manually
: ${RELEASE_NAME:={{ componentMetadata.name }}}
: ${NAMESPACE:={{ componentMetadata.envName }}}
: ${DEPLOYMENT_MODE:={{ flavourConfig.deploymentMode }}}

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

# Print the endpoint (this will be captured by the discovery stage)
echo "${ENDPOINT}"


