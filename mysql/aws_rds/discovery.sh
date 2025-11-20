#!/usr/bin/env bash
set -euo pipefail

DISCOVERY={}

reader_dns=$(jq -r '.readerEndpoint' state.json)
writer_dns=$(jq -r '.writerEndpoint' state.json)
DISCOVERY=$(echo ${DISCOVERY} | jq -c --arg reader "${reader_dns}" --arg writer "${writer_dns}" '.reader=$reader | .writer=$writer')

echo "${DISCOVERY}"
