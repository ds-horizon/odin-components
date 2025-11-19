#!/usr/bin/env bash
set -euo pipefail

DISCOVERY={}

# shellcheck disable=SC1083
discovery_private={{ baseConfig.discovery.private }}
# shellcheck disable=SC1083
discovery_public={{ baseConfig.discovery.public }}

if [[ ! -f "state.json" && "${PREVIOUS_STATE}" != "" ]]; then
  echo "${PREVIOUS_STATE}" > state.json
fi

if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
  discovery_private=$(jq -r '.deployConfig.discovery.private' state.json)
  discovery_public=$(jq -r '.deployConfig.discovery.public' state.json)
fi

NAMESPACE=$(jq -r '.deploymentNamespace' state.json)

if [[ ${discovery_private} != "null" ]]; then
  PRIVATE_ENDPOINT=$(kubectl get ingress gateway-int -n "${NAMESPACE}" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
  if [[ -n "${PRIVATE_ENDPOINT}" && "${PRIVATE_ENDPOINT}" != "null" ]]; then
    DISCOVERY=$(echo "${DISCOVERY}" | jq -c --arg dp "${PRIVATE_ENDPOINT}" '.private=$dp')
  fi
fi

if [[ ${discovery_public} != "null" ]]; then
  PUBLIC_ENDPOINT=$(kubectl get ingress gateway-ext -n "${NAMESPACE}" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
  if [[ -n "${PUBLIC_ENDPOINT}" && "${PUBLIC_ENDPOINT}" != "null" ]]; then
    DISCOVERY=$(echo "${DISCOVERY}" | jq -c --arg dp "${PUBLIC_ENDPOINT}" '.public=$dp')
  fi
fi

echo "${DISCOVERY}"
