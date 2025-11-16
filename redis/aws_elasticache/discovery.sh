#!/usr/bin/env bash
set -euo pipefail

DISCOVERY={}

primary_dns=$(jq -r '.primaryEndpoint' state.json)
reader_dns=$(jq -r '.readerEndpoint' state.json)
DISCOVERY=$(echo ${DISCOVERY} | jq -c --arg primary "$primary_dns" --arg reader "$reader_dns" '.primary=$primary | .reader=$reader')

echo ${DISCOVERY}
