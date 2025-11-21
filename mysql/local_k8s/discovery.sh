#!/usr/bin/env bash
set -euo pipefail

DISCOVERY={}

# shellcheck disable=SC1083
export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
# shellcheck disable=SC1083
export NAMESPACE={{ componentMetadata.envName }}
RELEASE_NAME=$(jq -r '.releaseName' state.json)

function get_endpoints() {
  kubectl get endpointslices -n ${NAMESPACE} -l app.kubernetes.io/instance="${RELEASE_NAME}",app.kubernetes.io/component="$1" | grep headless | awk '{gsub(":[0-9]+", "", $4); print $4}'
}

WRITER_ENDPOINT=$(get_endpoints primary)

# shellcheck disable=SC1083,SC1072,SC1073,SC1009
if [[ {{ flavourConfig.reader.replicaCount  }} -eq 0 ]]; then
  READER_ENDPOINT=${WRITER_ENDPOINT}
else
  READER_ENDPOINT=$(get_endpoints secondary)
fi

DISCOVERY=$(echo ${DISCOVERY} | jq -c --arg reader "${READER_ENDPOINT}" --arg writer "${WRITER_ENDPOINT}" '.reader=$reader | .writer=$writer')

echo "${DISCOVERY}"
