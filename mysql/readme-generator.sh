#!/usr/bin/env bash
set -euo pipefail

# Installs readme generator tool
go install github.com/marcusolsson/json-schema-docs@v0.2.1

# Generate root level README
json-schema-docs -schema schema.json -template README.md.tpl > README.md

for flavour in *; do
  if [[ ! -d ${flavour} || ! -f ${flavour}/schema.json ]]; then
    continue
  fi
  # Generate flavour level README
  json-schema-docs -schema ${flavour}/schema.json -template ${flavour}/README.md.tpl > ${flavour}/README.md

  if [[ -d ${flavour}/operations ]]; then
    for operation in ${flavour}/operations/*; do
        # Generate operation README
        json-schema-docs -schema ${operation}/schema.json -template ${operation}/README.md.tpl > ${operation}/README.md
      done
  fi

done