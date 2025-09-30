#!/usr/bin/env bash
set -euo pipefail

# Installs readme generator tool
go install github.com/marcusolsson/json-schema-docs@v0.2.1

echo "Starting README generation..."

# Find all schema.json files, excluding hidden directories
# Process each schema.json and check if corresponding README.md.tpl exists

while IFS= read -r schema_file; do
  # Get the directory containing the schema.json
  schema_dir=$(dirname "${schema_file}")

  # Check if README.md.tpl exists in the same directory
  template_file="${schema_dir}/README.md.tpl"

  if [[ -f "${template_file}" ]]; then
    echo "Generating ${schema_dir}/README.md"
    json-schema-docs -schema "${schema_file}" -template "${template_file}" > "${schema_dir}/README.md"
  fi
done < <(find . -name "schema.json" -type f -not -path "*/.*/*")

echo "README generation complete!"
