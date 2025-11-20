#!/usr/bin/env bash
set -euo pipefail
source ./logging.sh
setup_error_handling
{
export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
export NAMESPACE={{ componentMetadata.envName }}

if [[ -f state.json ]] && jq -e '.releaseName' state.json > /dev/null; then
    RELEASE_NAME=$(jq -r '.releaseName' state.json)
    echo "Using existing RELEASE_NAME from state.json: ${RELEASE_NAME}"
else
    echo "No state file found for component" 1>&2
    exit 1
fi 

function get_endpoints() {
  kubectl get endpointslices -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME},app.kubernetes.io/component=$1 | grep headless | awk '{gsub(":[0-9]+", "", $4); print $4}'
}

WRITER_ENDPOINT=$(get_endpoints primary)

if [[ {{ readerCount }} -eq 0 ]]; then
  READER_ENDPOINT=${WRITER_ENDPOINT}
else
  READER_ENDPOINT=$(get_endpoints secondary)
fi
echo "{\"writer\":\"${WRITER_ENDPOINT}\", \"reader\":\"${READER_ENDPOINT}\"}"
} 2> >(log_errors_with_timestamp) | log_with_timestamp
