#!/usr/bin/env bash
set -euo pipefail

DISCOVERY={}

discovery_private={{ baseConfig.discovery.private }}
discovery_public={{ baseConfig.discovery.public }}

if [[ ! -f "state.json" && "${PREVIOUS_STATE}" != "" ]]; then
  echo "${PREVIOUS_STATE}" > state.json
fi

if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
  discovery_private=$(jq -r '.deployConfig.discovery.private' state.json)
  discovery_public=$(jq -r '.deployConfig.discovery.public' state.json)
fi

if [[ ${discovery_private} != "null" ]]; then
  DISCOVERY=$(echo ${DISCOVERY} | jq -c --arg dp "$discovery_private" '.private=$dp')
fi
if [[ ${discovery_public} != "null" ]]; then
  DISCOVERY=$(echo ${DISCOVERY} | jq -c --arg dp "$discovery_public" '.public=$dp')
fi

echo ${DISCOVERY}
