#!/usr/bin/env bash
set -euo pipefail

JAR_FILE_PATH=mysql-aws_rds-jar-with-dependencies.jar
java -jar ${JAR_FILE_PATH} $1
