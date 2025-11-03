#!/usr/bin/env bash
set -euo pipefail

trap 'wait; exit 1' SIGTERM SIGINT

JAR_FILE_PATH=application-local-kubernetes.jar

if [[ -z $1 ]]; then
  echo "::error:: Please specify which operation to run" >&2
  exit 1
fi

java -jar "${JAR_FILE_PATH}" "$1"
