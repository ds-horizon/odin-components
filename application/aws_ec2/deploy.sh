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

JAR_FILE_PATH=application-aws-ec2.jar
PACKER_FILE_NAME=ami.pkr.hcl

# Download artifact
bash download_artifact.sh 2> >(log_errors_with_timestamp) | log_with_timestamp

# Extract docker registry data
# shellcheck disable=SC2155
export DOCKER_REGISTRIES=$(echo "${ODIN_COMPONENT_METADATA}" | jq '[.cloudProviderDetails.account.services[], (.cloudProviderDetails.linked_accounts[].services[])] | map(select(.category=="DOCKER_REGISTRY"))')

# Docker login
export DOCKER_CONFIG=/tmp
bash docker_login.sh 2> >(log_errors_with_timestamp) | log_with_timestamp

# Create AMI
java -jar "${JAR_FILE_PATH}" ami-template
if [[ -f ${PACKER_FILE_NAME} ]]; then
  bash packer.sh ${PACKER_FILE_NAME} 2> >(log_errors_with_timestamp) | log_with_timestamp
fi

bash execute_scripts.sh pre-deploy 2> >(log_errors_with_timestamp) | log_with_timestamp

# Start deployment
java -jar "${JAR_FILE_PATH}" "$1"

bash execute_scripts.sh post-deploy 2> >(log_errors_with_timestamp) | log_with_timestamp
