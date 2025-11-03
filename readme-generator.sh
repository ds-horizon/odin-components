#!/usr/bin/env bash
set -euo pipefail

# Install the fixed json-schema-docs from forked repository
# until https://github.com/marcusolsson/json-schema-docs/pull/5 is merged, we will use fork
echo "Installing json-schema-docs from https://github.com/yogesh-badke/json-schema-docs (commit: 14220ff)..."
go install github.com/yogesh-badke/json-schema-docs@14220ff34122155951a3f6693cb6e9460d1c017f

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
  else
    echo -e "\033[0;31mERROR: Found ${schema_file} but missing ${template_file}\033[0m"
    exit 1
  fi
done < <(find . -name "schema.json" -type f -not -path "*/.*/*")

echo "README generation complete!"
