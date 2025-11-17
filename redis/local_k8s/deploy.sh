#!/usr/bin/env bash
set -euo pipefail

{
  # Environment variables
  export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
  export RELEASE_NAME={{ componentMetadata.name }}
  export NAMESPACE={{ componentMetadata.envName }}
  export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode }}
  export REDIS_VERSION={{ baseConfig.version }}

  # Helper: wait for pods with a given name prefix to be Running & Ready
  wait_for_pods() {
    local pod_prefix=$1
    local expected_pods=$2
    local display_name=$3

    echo "Waiting for ${display_name} (expected pods: ${expected_pods})..."
    local max_wait=300 # up to 5 minutes
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

  # Script directory (for loading per-mode Helm values files)
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

  echo "================================================================"
  echo "Redis Deployment - Opstree Operator (local_k8s)"
  echo "================================================================"
  echo "Release Name: ${RELEASE_NAME}"
  echo "Namespace: ${NAMESPACE}"
  echo "Redis Version: ${REDIS_VERSION}"
  echo "Deployment Mode: ${DEPLOYMENT_MODE}"
  echo "================================================================"

  # Validate Redis version against Opstree operator support and deployment mode
  # Opstree supports Redis >= 6.2; we explicitly allow 6.2, 7.0, 7.1 from the root schema.
  if [[ "${REDIS_VERSION}" != "7.1" && "${REDIS_VERSION}" != "7.0" && "${REDIS_VERSION}" != "6.2" ]]; then
    echo "ERROR: Redis version ${REDIS_VERSION} is not supported by the Opstree Redis Operator for local_k8s." 1>&2
    echo "Supported versions for local_k8s flavour are: 6.2, 7.0, 7.1." 1>&2
    echo "Please update the root 'version' in redis/schema.json or choose a different flavour." 1>&2
    exit 1
  elif [[ "${DEPLOYMENT_MODE}" == "cluster" && "${REDIS_VERSION}" == "6.2" ]]; then
    echo "ERROR: Redis cluster mode is not supported with Redis version 6.2 in this flavour." 1>&2
    echo "Reason: the Opstree Redis cluster chart config uses 'cluster-announce-hostname'," 1>&2
    echo "which is not understood by Redis 6.2.x. Please use version 7.0 or 7.1 for cluster mode." 1>&2
    exit 1
  else
    echo "Redis version ${REDIS_VERSION} is supported by Opstree operator for deployment mode '${DEPLOYMENT_MODE}'."
  fi

  # Deploy Redis using Opstree Helm charts from ot-helm based on deployment mode
  echo "Deploying Redis using Helm charts"

  case "${DEPLOYMENT_MODE}" in
    standalone)
      echo "Deploying Redis in standalone mode via Helm..."
      HELM_RELEASE="${RELEASE_NAME}-standalone"
      VALUES_FILE="${SCRIPT_DIR}/values-standalone.yaml"

      helm upgrade "${HELM_RELEASE}" ot-helm/redis \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${VALUES_FILE}" \
        --wait \
        --timeout 10m

      # Standalone: expect exactly 1 Redis pod
      wait_for_pods "${RELEASE_NAME}-standalone" 1 "Redis Standalone"
      ;;

    sentinel)
      echo "Deploying Redis in sentinel mode via Helm..."

      # Replication (master + replicas)
      REPL_RELEASE="${RELEASE_NAME}-replication"
      REPL_VALUES_FILE="${SCRIPT_DIR}/values-sentinel-replication.yaml"

      helm upgrade "${REPL_RELEASE}" ot-helm/redis-replication \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${REPL_VALUES_FILE}" \
        --wait \
        --timeout 10m

      # Sentinel replication group pods (1 master + replicas)
      wait_for_pods "${RELEASE_NAME}-replication" {{ flavourConfig.sentinel.replicationSize }} "Redis Sentinel Replication"

      # Sentinel monitoring the replication (redis-sentinel chart)
      SENTINEL_RELEASE="${RELEASE_NAME}-sentinel"
      SENTINEL_VALUES_FILE="${SCRIPT_DIR}/values-sentinel-sentinel.yaml"

      helm upgrade "${SENTINEL_RELEASE}" ot-helm/redis-sentinel \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${SENTINEL_VALUES_FILE}" \
        --wait \
        --timeout 10m

      # Sentinel pods themselves
      wait_for_pods "${RELEASE_NAME}-sentinel" {{ flavourConfig.sentinel.sentinelSize }} "Redis Sentinel"
      ;;

    cluster)
      echo "Deploying Redis in cluster mode via Helm..."
      HELM_RELEASE="${RELEASE_NAME}-cluster"
      VALUES_FILE="${SCRIPT_DIR}/values-cluster.yaml"

      helm upgrade "${HELM_RELEASE}" ot-helm/redis-cluster \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${VALUES_FILE}" \
        --wait \
        --timeout 15m

      # Cluster leaders and followers:
      # - leaders: clusterSize
      # - followers: clusterSize * replicasPerMaster
      wait_for_pods "${RELEASE_NAME}-cluster-leader" {{ flavourConfig.cluster.clusterSize }} "Redis Cluster Leaders"
      wait_for_pods "${RELEASE_NAME}-cluster-follower" {{ flavourConfig.cluster.clusterSize * flavourConfig.cluster.replicasPerMaster }} "Redis Cluster Followers"
      ;;

    *)
      echo "ERROR: Unknown deployment mode: ${DEPLOYMENT_MODE}" 1>&2
      echo "Supported modes: standalone, sentinel, cluster" 1>&2
      exit 1
      ;;
  esac

  echo ""
  echo "================================================================"
  echo "Deployment completed"
  echo "================================================================"

}


