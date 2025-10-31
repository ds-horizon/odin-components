#!/usr/bin/env bash
set -euo pipefail

JAR_FILE_PATH=redis-aws-elasticache-jar-with-dependencies.jar
java -jar ${JAR_FILE_PATH} $1
