#!/usr/bin/env bash
set -euo pipefail

trap 'wait; exit 1' SIGTERM SIGINT

JAR_FILE_PATH=application-aws-container.jar

java -jar ${JAR_FILE_PATH} undeploy
