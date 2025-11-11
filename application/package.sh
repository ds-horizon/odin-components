#!/usr/bin/env bash
set -euo pipefail

# Local k8s
cd local_k8s
mvn --no-transfer-progress clean package -DskipTests
rm -rf src/ target/ pom.xml lombok.config README.md.tpl

cd .. # Return to component directory

# AWS EC2
cd aws_ec2
mvn --no-transfer-progress clean package -DskipTests
rm -rf src/ target/ pom.xml lombok.config README.md.tpl

cd .. # Return to component directory
