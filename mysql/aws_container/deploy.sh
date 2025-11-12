#!/usr/bin/env bash
set -euo pipefail
source ./logging.sh
source ./constants
setup_error_handling

update_state() {
  jq -n --arg name "${RELEASE_NAME}" --arg sha "${CURRENT_SHA}" \
    '{releaseName: $name, sha: $sha}' > state.json
}

trap 'update_state' EXIT

function print_marker() {
  echo "=========================================================================================="
}

if [[ -f state.json ]] && jq -e '.releaseName' state.json > /dev/null; then
  export PREVIOUS_SHA=$(jq -r '.sha' state.json)
  export RELEASE_NAME=$(jq -r '.releaseName' state.json)  
  echo "Using existing RELEASE_NAME from state.json: ${RELEASE_NAME}" | log_with_timestamp
else
  export PREVIOUS_SHA=""
  export RELEASE_NAME="{{ componentMetadata.name }}-${RANDOM}"
  echo "Generated new RELEASE_NAME: ${RELEASE_NAME}" | log_with_timestamp
fi
export CURRENT_SHA=$(sha256sum values.yaml | awk '{print $1}')

{

  export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
  export NAMESPACE={{ componentMetadata.envName }}
  export BASE_VERSION={{ baseConfig.version }}
  echo "PREVIOUS_SHA:${PREVIOUS_SHA}"
  echo "CURRENT_SHA:${CURRENT_SHA}"
  if [[ "${CURRENT_SHA}" == "${PREVIOUS_SHA}" ]]; then
    echo "No changes to apply"
  else
    helm repo add bitnami ${HELM_REPO}
    helm repo update
    helm upgrade --install ${RELEASE_NAME} bitnami/${HELM_CHART_NAME} --version ${HELM_CHART_VERSION} -n ${NAMESPACE} --values values.yaml --wait
    if [[ $? -ne 0 ]]; then
      echo "Mysql deployment failed. Please find pod description and logs below." 1>&2

      print_marker
      echo "Following pods were found"
      kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME}

      print_marker
      echo "Pod descriptions"
      kubectl describe pods -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME}

      print_marker
      echo "Pod logs"
      print_marker
      kubectl logs --since 5m -n ${NAMESPACE} -l app.kubernetes.io/instance=${RELEASE_NAME}

      # Exit with non zero error code
      exit 1
    fi
  fi

} 2> >(log_errors_with_timestamp) | log_with_timestamp
