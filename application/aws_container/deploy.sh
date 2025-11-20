#!/usr/bin/env bash
set -euo pipefail

trap 'wait; exit 1' SIGTERM SIGINT

log_with_timestamp() {
    awk '{
        cmd = "date +\"%Y-%m-%d %H:%M:%S,%3N\""
        cmd | getline timestamp
        close(cmd)
        if ($0 ~ /^::error::/) {
            print $0
        } else {
            print "::info::" timestamp " " $0
        }
        fflush()
    }'
}

log_errors_with_timestamp() {
    awk '{
        cmd = "date +\"%Y-%m-%d %H:%M:%S,%3N\""
        cmd | getline timestamp
        close(cmd)
        print "::error::" timestamp " " $0
        fflush()
    }'
}

JAR_FILE_PATH=application-aws-container.jar
PACKER_FILE_NAME=image.pkr.hcl

# Create namespace
java -jar ${JAR_FILE_PATH} create-namespace

# Download artifact
bash download_artifact.sh 2> >(log_errors_with_timestamp) | log_with_timestamp

# Extract docker registry data
DOCKER_REGISTRIES=$(echo "${COMPONENT_METADATA}" | jq '[.cloudProviderDetails.account.services[], (.cloudProviderDetails.linked_accounts[].services[])] | map(select(.category=="DOCKER_REGISTRY"))')
export DOCKER_REGISTRIES
DOCKER_REGISTRY_FOR_PUBLISHING=$(echo "${DOCKER_REGISTRIES}" | jq '[.[] | select(.data.allowPush == true)]')
export DOCKER_REGISTRY_FOR_PUBLISHING

# Docker login
export DOCKER_CONFIG=/tmp
# shellcheck disable=SC1091
source docker_login.sh 2> >(log_errors_with_timestamp) | log_with_timestamp

# # Create image
java -jar ${JAR_FILE_PATH} image-template
if [[ -f ${PACKER_FILE_NAME} ]]; then
  bash packer.sh ${PACKER_FILE_NAME} 2> >(log_errors_with_timestamp) | log_with_timestamp
fi

bash execute_scripts.sh pre-deploy 2> >(log_errors_with_timestamp) | log_with_timestamp

# # Start deployment
java -jar ${JAR_FILE_PATH} "$1"

bash execute_scripts.sh post-deploy 2> >(log_errors_with_timestamp) | log_with_timestamp
