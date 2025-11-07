#!/bin/bash
set -euo pipefail
source ./logging.sh
setup_error_handling

{
    export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
    export RELEASE_NAME={{ componentMetadata.name }}
    export NAMESPACE={{ componentMetadata.envName }}

    echo "Uninstalling mysql helm chart..."

    # Check if Helm release exists
    if helm list -n ${NAMESPACE} | grep -q "^$RELEASE_NAME[[:space:]]"; then
        echo "Found Helm release ${RELEASE_NAME} in namespace ${NAMESPACE}"

        echo "Uninstalling Helm chart..."
        if helm uninstall ${RELEASE_NAME} -n ${NAMESPACE} --wait; then
            echo "Helm release '$RELEASE_NAME' uninstalled successfully"
        else
            echo "Failed to uninstall Helm release ${RELEASE_NAME}" 1>&2
            exit 1
        fi
    else
        echo "Helm release ${RELEASE_NAME} not found in namespace ${NAMESPACE}"
    fi

    echo "Checking for persistent volume claims"
    PVC_LIST=$(kubectl get pvc -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME} | awk '(NR>1){print $1}')

    if [[ -z "${PVC_LIST}" ]]; then
        echo "No PVCs found for release ${RELEASE_NAME} in namespace ${NAMESPACE}"
        exit 0
    else
        echo "Deleting PVCs: ${PVC_LIST}"
        echo "${PVC_LIST}" | xargs kubectl delete pvc -n "${NAMESPACE}"
        echo "PVCs deleted successfully"
    fi

} 2> >(log_errors_with_timestamp) | log_with_timestamp
