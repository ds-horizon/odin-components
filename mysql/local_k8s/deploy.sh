#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=/dev/null
source ./logging.sh
# shellcheck source=/dev/null
source ./constants
setup_error_handling

update_state() {
  jq -n --arg name "${RELEASE_NAME}" \
    '{releaseName: $name}' > state.json
}

trap 'update_state' EXIT

function print_marker() {
  echo "=========================================================================================="
}

if [[ -f state.json ]] && jq -e '.releaseName' state.json > /dev/null; then
  export RELEASE_NAME
  RELEASE_NAME=$(jq -r '.releaseName' state.json)
  echo "Using existing RELEASE_NAME from state.json: ${RELEASE_NAME}" | log_with_timestamp
else
  export RELEASE_NAME="{{ componentMetadata.name }}-${RANDOM}"
  echo "Generated new RELEASE_NAME: ${RELEASE_NAME}" | log_with_timestamp
fi

{
  # shellcheck disable=SC1083
  export NAMESPACE={{ componentMetadata.envName }}
  # shellcheck disable=SC1083
  export BASE_VERSION={{ baseConfig.version }}
  # shellcheck disable=SC1083
  export IMAGE_TAG={{ baseConfig.version }}{{ flavourConfig.image.tagSuffix }}

  if ! helm upgrade --install --repo "${HELM_REPO}" "${RELEASE_NAME}" "${HELM_CHART_NAME}" --version "${HELM_CHART_VERSION}" -n ${NAMESPACE} --values values.yaml --set image.tag="${IMAGE_TAG}" --wait; then
    echo "Mysql deployment failed. Please find pod description and logs below." 1>&2

    print_marker
    echo "Following pods were found"
    kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/instance="${RELEASE_NAME}"

    print_marker
    echo "Pod descriptions"
    kubectl describe pods -n ${NAMESPACE} -l app.kubernetes.io/instance="${RELEASE_NAME}"

    print_marker
    echo "Pod logs"
    print_marker
    kubectl logs --since 5m -n ${NAMESPACE} -l app.kubernetes.io/instance="${RELEASE_NAME}"

    # Exit with non zero error code
    exit 1
  fi

} 2> >(log_errors_with_timestamp) | log_with_timestamp
