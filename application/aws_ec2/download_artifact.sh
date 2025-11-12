#!/usr/bin/env bash
set -euo pipefail

function download_from_jfrog() {
  # shellcheck disable=SC2155
  export ARTIFACTORY_SERVER=$(echo "${ARTIFACTORY_DATA}" | jq -r '.data.url')
  # shellcheck disable=SC2155
  export ARTIFACTORY_USERNAME=$(echo "${ARTIFACTORY_DATA}" | jq -r '.data.username')
  # shellcheck disable=SC2155
  export ARTIFACTORY_PASSWORD=$(echo "${ARTIFACTORY_DATA}" | jq -r '.data.password')
  # shellcheck disable=SC2155
  export ARTIFACTORY_REPOSITORY=$(echo "${ARTIFACTORY_DATA}" | jq -r '.services[] | select(.category == "STORAGE").data.artifacts.repository')

  echo "Downloading artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]"
  ARTIFACT_LAST_MODIFIED_URL="${ARTIFACTORY_SERVER}/api/storage/${ARTIFACTORY_REPOSITORY}/${ARTIFACT_NAME}/${ARTIFACT_VERSION}?lastModified"
  status=$(curl --location -u "${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD}" --request GET "${ARTIFACT_LAST_MODIFIED_URL}" --output response.json -s -w "%{http_code}\n")
  if [[ ${status} == "404" ]]; then
      echo "Artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}] does not exist" >&2
      exit 1
  elif [[ ! ${status} == "200" ]]; then
      echo "Failed to download artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]" >&2
      cat response.json >&2
      exit 1
  fi
  LATEST_ARTIFACT_URI=$(jq -r .uri response.json)
  ARTIFACT_URL="${ARTIFACTORY_SERVER}/${ARTIFACTORY_REPOSITORY}/${ARTIFACT_NAME}/${ARTIFACT_VERSION}/${LATEST_ARTIFACT_URI##*/}"
  status=$(curl --location  -u "${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD}" --request GET "${ARTIFACT_URL}" --output "${ARTIFACT_NAME}.zip" -s -w "%{http_code}\n")
  if [[ ! ${status} == "200" ]]; then
      echo "Failed to download artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]" >&2
      exit 1
  fi
}

# Extract artifact name and version
if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
  ARTIFACT_NAME=$(jq -r '.deployConfig.artifact.name' state.json)
  ARTIFACT_VERSION=$(jq -r '.deployConfig.artifact.version' state.json)
fi

if [[ $(echo "${CONFIG}" | jq -r '.artifact.name') != null ]]; then
  # shellcheck disable=SC2155
  export ARTIFACT_NAME=$(echo "${CONFIG}" | jq -r '.artifact.name')
fi
if [[ $(echo "${CONFIG}" | jq -r '.artifact.version') != null ]]; then
  # shellcheck disable=SC2155
  export ARTIFACT_VERSION=$(echo "${CONFIG}" | jq -r '.artifact.version')
fi

# Download from artifactory
# shellcheck disable=SC2155
export ARTIFACTORY_DATA=$(echo "${ODIN_COMPONENT_METADATA}" | jq '(.cloudProviderDetails.account | select(.services[]?.category == "STORAGE")) // (.cloudProviderDetails.linked_accounts[] | select(.services[]?.category == "STORAGE")) // error("Service with category STORAGE not found in account or linked accounts.")')
# shellcheck disable=SC2155
ARTIFACTORY_ACCOUNT_PROVIDER=$(echo "${ARTIFACTORY_DATA}" | jq -r '.provider' | tr '[:lower:]' '[:upper:]')

if [[ "${ARTIFACTORY_ACCOUNT_PROVIDER}" == "JFROG" ]]; then
  download_from_jfrog "${ARTIFACTORY_DATA}"
else
  echo "Invalid artifactory provider:${ARTIFACTORY_ACCOUNT_PROVIDER}" >&2
  exit 1
fi

# Successfully downloaded artifact. Extract and add SHA to state
echo "Downloaded artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]"
# shellcheck disable=SC2155
export ARTIFACT_SHA=$(sha256sum "${ARTIFACT_NAME}.zip" | cut -d " " -f1)
unzip -oq "${ARTIFACT_NAME}.zip"
if [[ ! -f state.json ]]; then
  echo "{}" > state.json
fi
jq '.image.artifactSha=env.ARTIFACT_SHA' state.json > state.tmp.json
mv state.tmp.json state.json
