#!/usr/bin/env bash
set -euo pipefail

if [[ "$1" == "deploy" ]]; then
  echo "::info::Deploying aws_flavour"
  # shellcheck disable=SC2050
  if [[ "{{ flavourConfig.error }}" == "true" ]]; then
    echo "::error::Error while deploying aws_flavour"
    exit 1
  fi
fi

if [[ "$1" == "undeploy" ]]; then
  echo "::info::Undeploying aws_flavour"
fi
