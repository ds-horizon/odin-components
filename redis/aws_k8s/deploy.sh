#!/usr/bin/env bash
# shellcheck disable=SC1083,SC2086,SC2155,SC1091,SC2035,SC2250
set -euo pipefail
source ./logging.sh
source ./constants
setup_error_handling

export CURRENT_SHA=""
# Environment variables
export NAMESPACE={{ componentMetadata.envName }}
export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode }}
export REDIS_VERSION={{ baseConfig.version }}
export RELEASE_NAME={{ componentMetadata.name }}

update_state() {
  status=$?
  if [[ $status -eq 0 ]]; then
    jq -n --arg name "${RELEASE_NAME}" --arg sha "${CURRENT_SHA}" \
      '{releaseName: $name, sha: $sha}' > state.json
  fi
}

trap 'update_state' EXIT

if [[ -f state.json ]] && jq -e '.releaseName' state.json > /dev/null; then
  export PREVIOUS_SHA=$(jq -r '.sha' state.json)
  export RELEASE_NAME=$(jq -r '.releaseName' state.json)
  echo "Using existing RELEASE_NAME from state.json: ${RELEASE_NAME}" | log_with_timestamp
else
  export PREVIOUS_SHA=""
  export RELEASE_NAME={{ componentMetadata.name }}
  echo "Generated new RELEASE_NAME: ${RELEASE_NAME}" | log_with_timestamp
fi


{


  # Helper: wait for pods with a given name prefix to be Running & Ready
  wait_for_pods() {
    local pod_prefix=$1
    local expected_pods=$2
    local display_name=$3

    echo "Waiting for ${display_name} (expected pods: ${expected_pods})..."
    local max_wait=1500 # up to 30 minutes
    local waited=0

    while [[ ${waited} -lt ${max_wait} ]]; do
      local pods
      pods=$(kubectl get pods -n "${NAMESPACE}" --no-headers 2>/dev/null | grep "^${pod_prefix}-" || true)

      local total_pods
      total_pods=$(printf "%s\n" "${pods}" | sed '/^$/d' | wc -l | tr -d ' ')

      local ready_pods
      ready_pods=$(printf "%s\n" "${pods}" | awk '$3=="Running" && $2 ~ /^[0-9]+\/[0-9]+$/ { split($2, a, "/"); if (a[1]==a[2]) print $0 }' | wc -l | tr -d ' ')

      echo "  Pods for ${display_name}: total=${total_pods}, ready=${ready_pods}/${expected_pods}"

      if [[ "${total_pods}" -ge "${expected_pods}" && "${ready_pods}" -ge "${expected_pods}" ]]; then
        echo "${display_name} deployed successfully (${ready_pods} pod(s) Running & Ready)"
        return 0
      fi

      sleep 5
      waited=$((waited + 5))
    done

    echo "ERROR: Timed out waiting for ${display_name} pods to become Ready (expected ${expected_pods})." 1>&2
    return 1
  }

  # Helper: Apply primary discovery annotations from componentMetadata
  # Workaround for Opstree operator v0.22.2 not supporting kubernetesConfig.service.annotations
  apply_service_annotations() {
    local helm_release=$1
    local service_name=$2
    
    echo "Applying primary discovery annotations to ${service_name}..." | log_with_timestamp
    
    # Extract only primary discovery annotations from componentMetadata
    PRIMARY_ANNOTATIONS=$(echo "${COMPONENT_METADATA}" | \
      jq -r '.cloudProviderDetails.linked_accounts[] | select(.provider == "Odin") | .services[] | select(.category == "DISCOVERY") | .data.discoveryAnnotations.primary // {} | to_entries | .[] | "\(.key)=\(.value)"' 2>/dev/null)
    
    if [[ -n "${PRIMARY_ANNOTATIONS}" ]]; then
      echo "${PRIMARY_ANNOTATIONS}" | while IFS= read -r annotation; do
        if [[ -n "${annotation}" ]]; then
          kubectl annotate svc "${service_name}" -n "${NAMESPACE}" "${annotation}" --overwrite 2>&1 | log_with_timestamp
        fi
      done
    fi
  }


  # Script directory (for loading per-mode Helm values files)
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

  echo "================================================================"
  echo "Redis Deployment - Opstree Operator"
  echo "================================================================"
  echo "Release Name: ${RELEASE_NAME}"
  echo "Namespace: ${NAMESPACE}"
  echo "Redis Version: ${REDIS_VERSION}"
  echo "Deployment Mode: ${DEPLOYMENT_MODE}"
  echo "================================================================"

  # Ensure Helm + ot-helm repo + Redis operator are available before deploying any CRs
  if ! command -v helm >/dev/null 2>&1; then
    echo "ERROR: 'helm' binary not found in PATH. Please install Helm v3 and retry." 1>&2
    exit 1
  fi

  # Validate Redis version against Opstree operator support and deployment mode
  # Opstree supports Redis >= 6.2; we explicitly allow 6.2, 7.0, 7.2 from the root schema.
  SUPPORTED_VERSIONS=("6.2" "7.0" "7.2")

  # Check if version is supported
  version_supported=false
  for version in "${SUPPORTED_VERSIONS[@]}"; do
    if [[ "${version}" == "${REDIS_VERSION}" ]]; then
      version_supported=true
      break
    fi
  done

  if [[ "${version_supported}" == "false" ]]; then
    echo "ERROR: Redis version ${REDIS_VERSION} is not supported by the Opstree Redis Operator." 1>&2
    echo "Supported versions for aws_k8s flavour are: ${SUPPORTED_VERSIONS[*]}." 1>&2
    echo "Please update the root 'version' in redis/schema.json or choose a different flavour." 1>&2
    exit 1
  fi

  # Check cluster mode compatibility
  if [[ "${DEPLOYMENT_MODE}" == "cluster" && "${REDIS_VERSION}" == "6.2" ]]; then
    echo "ERROR: Redis cluster mode is not supported with Redis version 6.2 in this flavour." 1>&2
    echo "Please use Redis version 7.0 or 7.2 for cluster deployments." 1>&2
    exit 1
  fi

  # Deploy Redis using Opstree Helm charts from ot-helm based on deployment mode
  echo "Deploying Redis using Helm (ot-helm)..."

  case "${DEPLOYMENT_MODE}" in
    standalone)
      echo "Deploying Redis in standalone mode via Helm..."
      HELM_RELEASE="${RELEASE_NAME}-${DEPLOYMENT_MODE}"
      VALUES_FILE="${SCRIPT_DIR}/values-standalone.yaml"

      helm upgrade "${HELM_RELEASE}" ${REDIS_CHART_NAME} \
        --repo ${HELM_REPO_URL} \
        --version ${REDIS_CHART_VERSION} \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${SCRIPT_DIR}/values-common.yaml" \
        -f "${VALUES_FILE}" \
        --wait \
        --timeout 15m

      # Standalone: expect exactly 1 Redis pod
      wait_for_pods "${RELEASE_NAME}-${DEPLOYMENT_MODE}" 1 "Redis Standalone"
      
      # Apply service annotations
      apply_service_annotations "${HELM_RELEASE}" "${RELEASE_NAME}-${DEPLOYMENT_MODE}"
      ;;

    sentinel)
      echo "Deploying Redis in sentinel mode via Helm..."

      # Replication (master + replicas)
      REPL_RELEASE="${RELEASE_NAME}-replication"
      REPL_VALUES_FILE="${SCRIPT_DIR}/values-sentinel-replication.yaml"

      helm upgrade "${REPL_RELEASE}" ${REDIS_REPLICATION_CHART_NAME} \
        --repo ${HELM_REPO_URL} \
        --version ${REDIS_REPLICATION_CHART_VERSION} \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${SCRIPT_DIR}/values-common.yaml" \
        -f "${REPL_VALUES_FILE}" \
        --wait \
        --timeout 15m

      # Sentinel replication group pods (1 master + replicas)
      wait_for_pods "${RELEASE_NAME}-replication" {{ flavourConfig.sentinel.replicationSize }} "Redis Sentinel Replication"

      # Sentinel monitoring the replication (redis-sentinel chart)
      SENTINEL_RELEASE="${RELEASE_NAME}-${DEPLOYMENT_MODE}"
      SENTINEL_VALUES_FILE="${SCRIPT_DIR}/values-sentinel-sentinel.yaml"

      helm upgrade "${SENTINEL_RELEASE}" ${REDIS_SENTINEL_CHART_NAME} \
        --repo ${HELM_REPO_URL} \
        --version ${REDIS_SENTINEL_CHART_VERSION} \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${SCRIPT_DIR}/values-common.yaml" \
        -f "${SENTINEL_VALUES_FILE}" \
        --wait \
        --timeout 15m

      # Sentinel pods themselves
      wait_for_pods "${RELEASE_NAME}-${DEPLOYMENT_MODE}-sentinel" {{ flavourConfig.sentinel.sentinelSize }} "Redis Sentinel"
      
      # Apply primary discovery annotations to sentinel service only
      apply_service_annotations "${SENTINEL_RELEASE}" "${RELEASE_NAME}-${DEPLOYMENT_MODE}-sentinel"
      ;;

    cluster)
      echo "Deploying Redis in cluster mode via Helm..."
      HELM_RELEASE="${RELEASE_NAME}-${DEPLOYMENT_MODE}"
      VALUES_FILE="${SCRIPT_DIR}/values-cluster.yaml"

      helm upgrade "${HELM_RELEASE}" ${REDIS_CLUSTER_CHART_NAME} \
        --repo ${HELM_REPO_URL} \
        --version ${REDIS_CLUSTER_CHART_VERSION} \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${SCRIPT_DIR}/values-common.yaml" \
        -f "${VALUES_FILE}" \
        --wait \
        --timeout 20m

      # Cluster leaders and followers:
      # - leaders: clusterSize
      # - followers: clusterSize * replicasPerMaster
      wait_for_pods "${RELEASE_NAME}-${DEPLOYMENT_MODE}-leader" {{ flavourConfig.cluster.clusterSize }} "Redis Cluster Leaders"
      wait_for_pods "${RELEASE_NAME}-${DEPLOYMENT_MODE}-follower" {{ flavourConfig.cluster.clusterSize * flavourConfig.cluster.replicasPerMaster }} "Redis Cluster Followers"
      
      # Apply primary discovery annotations to cluster master service only
      apply_service_annotations "${HELM_RELEASE}" "${RELEASE_NAME}-${DEPLOYMENT_MODE}-master"
      ;;

    *)
      echo "ERROR: Unknown deployment mode: ${DEPLOYMENT_MODE}" 1>&2
      echo "Supported modes: standalone, sentinel, cluster" 1>&2
      exit 1
      ;;
  esac

  echo ""
  echo "================================================================"
  echo "Deployment completed!"
  echo "================================================================"

} 2> >(log_errors_with_timestamp) | log_with_timestamp
