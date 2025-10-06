#!/usr/bin/env bash
set -eo pipefail

for arg in "$@"
do
  IFS='=' read -r key value <<< "$arg"
  declare "$key=$value"
done

echo -e "\ncomponent: $component"
echo -e "stage: $stage"
echo -e "account_flavour: $account_flavour\n"

if [[ -z ${component} || -z ${stage} || -z ${account_flavour} ]]; then
  echo -e "Please provide the component, stage and account_flavour, example command:"
  echo -e "bash run.sh component=mysql stage=deploy account_flavour=stag_aws_rds\n"
  exit 1
fi

if [[ $stage == "operate" ]]; then
  if [[ -z ${operation} ]]; then
    echo -e "Please provide the operation\n"
    exit 1
  fi
  echo -e "operation: $operation\n"
fi

cd ${component}

export BASE_CONFIG=$(cat example/${account_flavour}/base_config.json | COMPONENT=${component} envsubst)
export FLAVOUR_CONFIG=$(cat example/${account_flavour}/flavour_config.json | COMPONENT=${component} envsubst)
export OPERATION_CONFIG=$(cat example/${account_flavour}/operation_config.json | COMPONENT=${component} envsubst)

export COMPONENT_METADATA=$(cat example/${account_flavour}/componentMetadata.json | COMPONENT=${component} envsubst)
export DSL_METADATA=$(cat example/${account_flavour}/dslMetadata.json | STAGE=${stage} OPERATION_NAME=${operation} COMPONENT=${component} envsubst)
export LOG_LEVEL=debug
groovy -cp ${PATH_TO_JAR} -DstrictHealthCheck=true component.groovy
