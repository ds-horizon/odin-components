#!/usr/bin/env bash
set -euo pipefail

cleanup() {
    if [ $? -ne 0 ]; then
        echo "AMI creation failed. Check the deployment logs for more details" >&2
    fi
}

trap 'cleanup' EXIT

PACKER_FILE_NAME=$1
echo "Starting AMI building process for ${PACKER_FILE_NAME}"
packer init ${PACKER_FILE_NAME}
packer build ${PACKER_FILE_NAME}

if [ -f "manifest.json" ]; then
    AMI_IDS=$(jq -r '.builds[].artifact_id' manifest.json | cut -d ":" -f2)
    AMI_ARCHITECTURES=$(jq -r '.builds[].name' manifest.json)
    for index in "${!AMI_IDS[@]}"; do
        AMI_ID=${AMI_IDS[$index]}
        ARCHITECTURE=${AMI_ARCHITECTURES[$index]}
        jq --arg ami_id "$AMI_ID" --arg architecture "$ARCHITECTURE" '
            if (.image.amis | any(.architecture == $architecture)) then
                (.image.amis[] | select(.architecture == $architecture) | .id) = $ami_id
            else
                .image.amis += [{"id": $ami_id, "architecture": $architecture}]
            end
            ' state.json > state.tmp.json
        mv state.tmp.json state.json
    done
    rm -rf manifest.json
fi
