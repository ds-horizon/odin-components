#!/usr/bin/env bash
set -eo pipefail

component=mysql
env="test-env"

for arg in "$@"
do
  IFS='=' read -r key value <<< "$arg"
  declare "$key=$value"
done

echo -e "\nstage: $stage"
echo "operation: $operation"
echo "account_flavour: $account_flavour"
echo "component: $component"
echo "env: $env"

if [[ -z ${stage} ]]; then
  echo -e "\nPlease provide the stage name, example command:"
  echo -e "bash run.sh stage=operate operation=redeploy account_flavour=flavour\n"
  exit 1
fi

export BASE_CONFIG=$(cat example/${account_flavour}/base_config.json | COMPONENT=${component} ENV=${env} envsubst)
export FLAVOUR_CONFIG=$(cat example/${account_flavour}/flavour_config.json | COMPONENT=${component} ENV=${env} envsubst)
export OPERATION_CONFIG=$(cat example/${account_flavour}/operation_config.json | COMPONENT=${component} ENV=${env} envsubst)

export COMPONENT_METADATA=$(cat example/${account_flavour}/componentMetadata.json | COMPONENT=${component} ENV=${env} envsubst)
export DSL_METADATA=$(cat example/${account_flavour}/dslMetadata.json | STAGE=${stage} OPERATION_NAME=${operation} COMPONENT=${component} ENV=${env} envsubst)
export LOG_LEVEL=debug
groovy -cp ${PATH_TO_JAR} -DstrictHealthCheck=true component.groovy