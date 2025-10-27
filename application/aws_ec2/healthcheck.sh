#!/usr/bin/env bash
set -euo pipefail

JAR_FILE_PATH=application-aws-ec2.jar

java -jar ${JAR_FILE_PATH} status
