#!/usr/bin/env bash
set -euo pipefail

if [ "$(echo "${DOCKER_REGISTRY_FOR_PUBLISHING}" | jq 'length')" -ne 1 ]; then
  echo "Error: Expected exactly one DOCKER_REGISTRY with allowPush=true" >&2
  exit 1
fi

AUTH_BLOCK=$(echo "${DOCKER_REGISTRIES}" | jq -r '
  map(select(.data.password != null and .data.password != ""))
    | map({
        (.data.server): {
          auth: ((.data.username + ":" + .data.password) | @base64)
        }
      })
    | add // {}
  ')

echo "{\"auths\": ${AUTH_BLOCK}}" > "${DOCKER_CONFIG}/config.json"
