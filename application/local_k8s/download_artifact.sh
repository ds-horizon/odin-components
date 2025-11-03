#!/usr/bin/env bash
set -euo pipefail

function download_from_jfrog() {
  export ARTIFACTORY_SERVER=$(echo ${ARTIFACTORY_DATA} | jq -r '.data.url')
  export ARTIFACTORY_USERNAME=$(echo ${ARTIFACTORY_DATA} | jq -r '.data.username')
  export ARTIFACTORY_PASSWORD=$(echo ${ARTIFACTORY_DATA} | jq -r '.data.password')
  export ARTIFACTORY_REPOSITORY=$(echo ${ARTIFACTORY_DATA} | jq -r '.services[] | select(.category == "STORAGE").data.artifacts.repository')

  echo "Downloading artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]"
  ARTIFACT_LAST_MODIFIED_URL="${ARTIFACTORY_SERVER}/api/storage/${ARTIFACTORY_REPOSITORY}/${ARTIFACT_NAME}/${ARTIFACT_VERSION}?lastModified"
  status=$(curl --location -u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} --request GET ${ARTIFACT_LAST_MODIFIED_URL} --output response.json -s -w "%{http_code}\n")
  if [[ ${status} == "404" ]]; then
      echo "Artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}] does not exist" >&2
      exit 1
  elif [[ ! ${status} == "200" ]]; then
      echo "Failed to download artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]" >&2
      cat response.json >&2
      exit 1
  fi
  LATEST_ARTIFACT_URI=$(cat response.json | jq -r .uri)
  ARTIFACT_URL="${ARTIFACTORY_SERVER}/${ARTIFACTORY_REPOSITORY}/${ARTIFACT_NAME}/${ARTIFACT_VERSION}/${LATEST_ARTIFACT_URI##*/}"
  status=$(curl --location  -u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} --request GET ${ARTIFACT_URL} --output ${ARTIFACT_NAME}.zip -s -w "%{http_code}\n")
  if [[ ! ${status} == "200" ]]; then
      echo "Failed to download artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]" >&2
      exit 1
  fi
}


# Create artifact cache directory
# shellcheck disable=SC1064,SC1065,SC1073,SC1072,SC1083
MOUNT_PATH={{ componentMetadata.cloudProviderDetails.account.data.homeDirectoryMountPath | default("/host") }}
CACHE_DIRECTORY="${MOUNT_PATH}/.odin/cache/application/artifacts"
mkdir -p ${CACHE_DIRECTORY}

# Extract artifact name and version
if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
  ARTIFACT_NAME=$(jq -r '.deployConfig.artifact.name' state.json)
  ARTIFACT_VERSION=$(jq -r '.deployConfig.artifact.version' state.json)
fi

if [[ $(echo "${CONFIG}" | jq -r '.artifact.name') != null ]]; then
  export ARTIFACT_NAME=$(echo "${CONFIG}" | jq -r '.artifact.name')
fi
if [[ $(echo "${CONFIG}" | jq -r '.artifact.version') != null ]]; then
  export ARTIFACT_VERSION=$(echo "${CONFIG}" | jq -r '.artifact.version')
fi

ARTIFACT_CACHE_PATH=${CACHE_DIRECTORY}/${ARTIFACT_NAME}/${ARTIFACT_VERSION}

# Check if local artifactory is being used
if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
  LOCAL_ARTIFACT_ENABLED=$(jq -r '.deployConfig.localArtifact.enabled' state.json)
  LOCAL_ARTIFACT_PATH=$(jq -r '.deployConfig.localArtifact.path' state.json)
fi
if [[ $(echo "${CONFIG}" | jq -r '.localArtifact.enabled') != null ]]; then
  LOCAL_ARTIFACT_ENABLED=$(echo "${CONFIG}" | jq -r '.localArtifact.enabled')
fi
if [[ $(echo "${CONFIG}" | jq -r '.localArtifact.path') != null ]]; then
  LOCAL_ARTIFACT_PATH=$(echo "${CONFIG}" | jq -r '.localArtifact.path')
fi

if [[ "${LOCAL_ARTIFACT_ENABLED}" == "true" ]]; then
  # Use local artifact
  echo "Using artifact from local directory ${LOCAL_ARTIFACT_PATH}"
  cp -r ${MOUNT_PATH}/${LOCAL_ARTIFACT_PATH} ${ARTIFACT_NAME}
  export ARTIFACT_SHA="local"
else
  # Check artifact in cache
  if [[ -d ${ARTIFACT_CACHE_PATH} ]]; then
    # Artifact present in cache
    echo "Using artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}] from cache"
    cp -r ${ARTIFACT_CACHE_PATH}/* .
    export ARTIFACT_SHA=$(jq -r '.artifactSha' ${ARTIFACT_CACHE_PATH}/metadata.json)
  else
    # Download from artifactory
    export ARTIFACTORY_DATA=$(echo ${ODIN_COMPONENT_METADATA} | jq '(.cloudProviderDetails.account | select(.services[]?.category == "STORAGE")) // (.cloudProviderDetails.linked_accounts[] | select(.services[]?.category == "STORAGE") // error("Service with category STORAGE not found in account or linked accounts."))')
    ARTIFACTORY_ACCOUNT_PROVIDER=$(echo ${ARTIFACTORY_DATA} | jq -r '.provider' | tr '[:lower:]' '[:upper:]')

    if [[ "${ARTIFACTORY_ACCOUNT_PROVIDER}" == "JFROG" ]]; then
      download_from_jfrog ${ARTIFACTORY_DATA}
    else
      echo "Invalid artifactory provider:${ARTIFACTORY_ACCOUNT_PROVIDER}" >&2
      exit 1
    fi

    # Successfully downloaded artifact. Extract and add SHA to state
    echo "Downloaded artifact:[${ARTIFACT_NAME}] with version:[${ARTIFACT_VERSION}]"
    export ARTIFACT_SHA=$(sha256sum ${ARTIFACT_NAME}.zip | cut -d " " -f1)
    unzip -oq "${ARTIFACT_NAME}.zip"

    # Add artifact to cache
    echo "Adding downloaded artifact to cache"
    mkdir -p ${ARTIFACT_CACHE_PATH}
    cp -r ${ARTIFACT_NAME} ${ARTIFACT_CACHE_PATH}
    echo "{\"artifactSha\":\"${ARTIFACT_SHA}\"}" > ${ARTIFACT_CACHE_PATH}/metadata.json
  fi
fi

if [[ ! -f state.json ]]; then
  echo "{}" > state.json
fi

if [[ -n ${ARTIFACT_SHA} ]]; then
  jq '.image.artifactSha=env.ARTIFACT_SHA' state.json > state.tmp.json
  mv state.tmp.json state.json
fi
