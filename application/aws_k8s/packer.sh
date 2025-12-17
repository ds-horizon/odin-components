#!/usr/bin/env bash
set -euo pipefail

cleanup() {
    if [ $? -ne 0 ]; then
        echo "Image creation failed. Check the deployment logs for more details" >&2
    fi
}

trap 'cleanup' EXIT

PACKER_FILE_NAME="image.pkr.hcl"
echo "Starting image building process for ${PACKER_FILE_NAME}"
packer init ${PACKER_FILE_NAME}
packer build ${PACKER_FILE_NAME}

if [ -f "manifest.json" ]; then
    REGISTRY=$(jq -r '.builds[0].custom_data.registry' manifest.json)
    IMAGE_NAME=$(jq -r '.builds[0].custom_data.image_name' manifest.json)
    IMAGE_TAG=$(jq -r '.builds[0].custom_data.image_tag' manifest.json)

    docker manifest create "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}" --amend "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}-amd64" --amend "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}-arm64"
    docker manifest push "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

    rm -rf manifest.json
fi
