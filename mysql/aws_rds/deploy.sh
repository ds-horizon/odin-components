#!/usr/bin/env bash
set -euo pipefail

log_with_timestamp() {
    awk '{
        cmd = "date +\"%Y-%m-%d %H:%M:%S,%3N\""
        cmd | getline timestamp
        close(cmd)
        if ($0 ~ /^::error::/) {
            print $0
        } else {
            print "::info::" timestamp " " $0
        }
        fflush()
    }'
}

log_errors_with_timestamp() {
    awk '{
        cmd = "date +\"%Y-%m-%d %H:%M:%S,%3N\""
        cmd | getline timestamp
        close(cmd)
        print "::error::" timestamp " " $0
        fflush()
    }'
}

JAR_FILE_PATH=mysql-aws_rds-jar-with-dependencies.jar

# Start deployment
java -jar ${JAR_FILE_PATH} deploy
