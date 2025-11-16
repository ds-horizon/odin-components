#!/usr/bin/env bash
set -euo pipefail

{
  # Environment variables
  export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
  export RELEASE_NAME={{ componentMetadata.name }}
  export NAMESPACE={{ componentMetadata.envName }}
  export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode }}
  export REDIS_VERSION={{ baseConfig.version }}


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

  # Validate Redis version against Opstree operator support
  # Opstree supports Redis >= 6.2; we explicitly allow 6.2, 7.0, 7.1 from the root schema.
  case "${REDIS_VERSION}" in
    7.1|7.0|6.2)
      echo "Redis version ${REDIS_VERSION} is supported by Opstree operator."
      ;;
    *)
      echo "ERROR: Redis version ${REDIS_VERSION} is not supported by the Opstree Redis Operator." 1>&2
      echo "Supported versions for aws_k8s flavour are: 6.2, 7.0, 7.1." 1>&2
      echo "Please update the root 'version' in redis/schema.json or choose a different flavour." 1>&2
      exit 1
      ;;
  esac

  # Check if Opstree Redis Operator is installed
  echo "Checking Opstree Redis Operator installation..."
  if ! kubectl get deployment redis-operator -n redis-operator &>/dev/null; then
    echo "Opstree Redis Operator not found. Installing..."
    helm repo add ot-helm https://ot-container-kit.github.io/helm-charts/ --force-update
    helm repo update
    helm upgrade --install redis-operator ot-helm/redis-operator \
      --namespace ${NAMESPACE} \
      --wait \
      --timeout 5m
    echo "Opstree Redis Operator installed successfully"
  else
    echo "Opstree Redis Operator already installed"
  fi

  # Deploy Redis using Opstree Helm charts from ot-helm based on deployment mode
  echo "Deploying Redis using Helm (ot-helm)..."

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
        --timeout 15m
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
        --timeout 15m

      # Sentinel monitoring the replication (redis-sentinel chart)
      SENTINEL_RELEASE="${RELEASE_NAME}-sentinel"
      SENTINEL_VALUES_FILE="${SCRIPT_DIR}/values-sentinel-sentinel.yaml"

      helm upgrade "${SENTINEL_RELEASE}" ot-helm/redis-sentinel \
        --install \
        --namespace "${NAMESPACE}" \
        -f "${SENTINEL_VALUES_FILE}" \
        --wait \
        --timeout 15m
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
        --timeout 20m
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

}
