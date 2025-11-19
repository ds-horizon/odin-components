#!/usr/bin/env bash
set -euo pipefail

JAR_FILE_PATH=application-aws-container-v2-jar-with-dependencies.jar

java -jar ${JAR_FILE_PATH} status
