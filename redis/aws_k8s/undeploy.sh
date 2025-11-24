#!/usr/bin/env bash
set -euo pipefail
source ./logging.sh
setup_error_handling

{
  # Environment variables
  export RELEASE_NAME={{ componentMetadata.name }}
  export NAMESPACE={{ componentMetadata.envName }}
  export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode | default('standalone') }}

  if [[ -f state.json ]] && jq -e '.releaseName' state.json > /dev/null; then
    RELEASE_NAME=$(jq -r '.releaseName' state.json)
    echo "Using existing RELEASE_NAME from state.json: ${RELEASE_NAME}"
  else
    echo "No state file found for component" 1>&2
    exit 1
  fi

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
      helm uninstall "${RELEASE_NAME}-cluster" -n "${NAMESPACE}" --wait || echo "Helm release ${RELEASE_NAME}-cluster not found"
      ;;
    *)
      echo "WARNING: Unknown deployment mode: ${DEPLOYMENT_MODE}" 1>&2
      ;;
  esac



  if [[ "${DEPLOYMENT_MODE}" == "sentinel" ]]; then
    # Replication chart creates the PVCs; sentinel chart typically does not.
    INSTANCE_NAME="${RELEASE_NAME}-replication"
  else
    INSTANCE_NAME="${RELEASE_NAME}-${DEPLOYMENT_MODE}"
  fi
  echo "Checking for PVCs"
  PVC_NAMES=$(kubectl get pvc -n ${NAMESPACE} -l app.kubernetes.io/instance=${INSTANCE_NAME} | awk '(NR>1){print $1}')

  if [[ -z "${PVC_NAMES}" ]]; then
    echo "No PVCs found for release ${RELEASE_NAME}"
  else
    echo "Deleting PVCs: ${PVC_NAMES}"
    echo "${PVC_NAMES}" | xargs kubectl delete pvc -n "${NAMESPACE}"
    echo "PVCs deleted successfully"
  fi


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

  if [[ -f state.json ]]; then
    echo "Resetting state.json for component..."
    echo '{}' > state.json || true
  fi

} 2> >(log_errors_with_timestamp) | log_with_timestamp
