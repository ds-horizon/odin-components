#!/usr/bin/env bash
set -euo pipefail

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
