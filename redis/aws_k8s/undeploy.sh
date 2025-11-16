#!/usr/bin/env bash
set -euo pipefail
# source ./logging.sh
# setup_error_handling

{
  # Environment variables
  export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
  export RELEASE_NAME={{ componentMetadata.name }}
  export NAMESPACE={{ componentMetadata.envName }}
  export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode | default('standalone') }}

  echo "================================================================"
  echo "Redis Undeployment - Opstree Operator (Helm-managed)"
  echo "================================================================"
  echo "Release Name: ${RELEASE_NAME}"
  echo "Namespace: ${NAMESPACE}"
  echo "Deployment Mode: ${DEPLOYMENT_MODE}"
  echo "================================================================"
  echo ""

  # Check if namespace exists
  if ! kubectl get namespace ${NAMESPACE} &>/dev/null; then
    echo "Namespace ${NAMESPACE} not found. Nothing to undeploy."
    exit 0
  fi

  # Uninstall Helm releases based on deployment mode
  case "${DEPLOYMENT_MODE}" in
    standalone)
      echo "Uninstalling Redis standalone Helm release..."
      helm uninstall "${RELEASE_NAME}-standalone" -n "${NAMESPACE}" --wait || echo "Helm release ${RELEASE_NAME}-standalone not found"
      ;;
    sentinel)
      echo "Uninstalling Redis Sentinel Helm releases..."
      helm uninstall "${RELEASE_NAME}-sentinel" -n "${NAMESPACE}" --wait || echo "Helm release ${RELEASE_NAME}-sentinel not found"
      helm uninstall "${RELEASE_NAME}-replication" -n "${NAMESPACE}" --wait || echo "Helm release ${RELEASE_NAME}-replication not found"
      ;;
    cluster)
      echo "Uninstalling Redis Cluster Helm release..."
      helm uninstall "${RELEASE_NAME}-cluster" -n "${NAMESPACE}" --wait|| echo "Helm release ${RELEASE_NAME}-cluster not found"
      ;;
    *)
      echo "WARNING: Unknown deployment mode: ${DEPLOYMENT_MODE}" 1>&2
      ;;    
  esac

  # Delete unbound PVCs that belong to this release (leftover from operator defaults)
  echo ""
  echo "Checking for unbound PVCs for release ${RELEASE_NAME}..."
  UNBOUND_PVCS=$(kubectl get pvc -n "${NAMESPACE}" --no-headers 2>/dev/null | grep "${RELEASE_NAME}-${DEPLOYMENT_MODE}" | awk '$2!="Bound"{print $1}' || true)
  if [[ -n "${UNBOUND_PVCS}" ]]; then
    echo "Found unbound PVC(s) to delete:"
    echo "${UNBOUND_PVCS}"
    kubectl delete pvc -n "${NAMESPACE}" ${UNBOUND_PVCS} || true
  else
    echo "No unbound PVCs found for release ${RELEASE_NAME}"
  fi

  # Delete the operator in this namespace (if installed there)
  helm uninstall redis-operator -n ${NAMESPACE} || echo "Helm release redis-operator not found" 


  echo ""
  echo "================================================================"
  echo "Undeployment Status (Helm + Operator)"
  echo "================================================================"
  echo "Remaining Pods (if any):"
  kubectl get pods -n ${NAMESPACE} 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" || echo "None"
  echo ""
  echo "================================================================"
  echo "Undeployment completed!"
  echo "================================================================"
  echo ""

}

