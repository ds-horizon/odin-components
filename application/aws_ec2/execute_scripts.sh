#!/usr/bin/env bash
set -euo pipefail

# Extract artifact name
if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
    ARTIFACT_NAME=$(jq -r '.deployConfig.artifact.name' state.json)
fi
if [[ $(echo "${CONFIG}" | jq -r '.artifact.name') != null ]]; then
    ARTIFACT_NAME=$(echo "${CONFIG}" | jq -r '.artifact.name')
fi

if [[ ! -d ${ARTIFACT_NAME} ]]; then
    echo "Extracted directory for artifact:[${ARTIFACT_NAME}] does not exist" >&2
    exit 1
fi

if [[ "$1" == "pre-deploy" ]]; then
    HOOK_ENABLED_VAR=HOOK_PRE_DEPLOY_ENABLED
    if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
        file_path=$(jq -r '.deployConfig.artifact.hooks.preDeploy.script' state.json)
        enabled=$(jq -r '.deployConfig.artifact.hooks.preDeploy.enabled' state.json)
        docker_image=$(jq -r '.deployConfig.artifact.hooks.preDeploy.dockerImage' state.json)
    fi
    if [[ $(echo "${CONFIG}" | jq -r '.artifact.hooks.preDeploy.script') != null ]]; then
        file_path=$(echo "${CONFIG}" | jq -r '.artifact.hooks.preDeploy.script')
    fi
    if [[ $(echo "${CONFIG}" | jq -r '.artifact.hooks.preDeploy.enabled') != null ]]; then
        enabled=$(echo "${CONFIG}" | jq -r '.artifact.hooks.preDeploy.enabled')
    fi
    if [[ $(echo "${CONFIG}" | jq -r '.artifact.hooks.preDeploy.dockerImage') != null ]]; then
        docker_image=$(echo "${CONFIG}" | jq -r '.artifact.hooks.preDeploy.dockerImage')
    fi
elif [[ "$1" == "post-deploy" ]]; then
    HOOK_ENABLED_VAR=HOOK_POST_DEPLOY_ENABLED
    if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
        file_path=$(jq -r '.deployConfig.artifact.hooks.postDeploy.script' state.json)
        enabled=$(jq -r '.deployConfig.artifact.hooks.postDeploy.enabled' state.json)
        docker_image=$(jq -r '.deployConfig.artifact.hooks.postDeploy.dockerImage' state.json)
    fi
    if [[ $(echo "${CONFIG}" | jq -r '.artifact.hooks.postDeploy.script') != null ]]; then
        file_path=$(echo "${CONFIG}" | jq -r '.artifact.hooks.postDeploy.script')
    fi
    if [[ $(echo "${CONFIG}" | jq -r '.artifact.hooks.postDeploy.enabled') != null ]]; then
        enabled=$(echo "${CONFIG}" | jq -r '.artifact.hooks.postDeploy.enabled')
    fi
    if [[ $(echo "${CONFIG}" | jq -r '.artifact.hooks.postDeploy.dockerImage') != null ]]; then
        docker_image=$(echo "${CONFIG}" | jq -r '.artifact.hooks.postDeploy.dockerImage')
    fi
else
    echo "[$1] is not a valid script"
    exit 1
fi

if [[ ${enabled} == true ]]; then
    # shellcheck disable=SC1064,SC1065,SC1073,SC1072,SC1083,SC1054,SC1009
    {% set ec2_data = componentMetadata.cloudProviderDetails.account.services | selectattr('category', 'eq', 'VM') | list | first %}
    # shellcheck disable=SC1064,SC1065,SC1073,SC1072,SC1083,SC1054
    {% set env_variables = ec2_data.data.environmentVariables | default({}, true) %}
    env_variables="{{ env_variables | tojson | replace('"', '\\"') }}"
    env_variables=$(echo "${env_variables}" | jq '. + {"APP_DIR": "/tmp/'"${ARTIFACT_NAME}"'", "DEPLOYMENT_TYPE": "aws_ec2"}')
    if [[ -f "state.json" && $(jq -r '.deployConfig' state.json) != null ]]; then
        env_variables=$(echo "${env_variables}" | jq --argjson vars "$(jq -r '.deployConfig.extraEnvVars' state.json)" '. * $vars')
    fi
    if [[ $(echo $CONFIG | jq -r '.extraEnvVars') != null ]]; then
        env_variables=$(echo "${env_variables}" "${CONFIG}" | jq -s '.[0] * .[1].extraEnvVars')
    fi

    # Check if HOOK_ENABLED_VAR is set
    HOOK_ENABLED=$(echo ${env_variables} | jq -r ".$HOOK_ENABLED_VAR")
    if [[ ${HOOK_ENABLED} == "false" ]]; then
        echo "Skipping the execution of [$1] script as ${HOOK_ENABLED_VAR} is set to false"
        exit 0
    fi
    echo "Executing [$1] script"
    echo "${env_variables}" | jq -r 'to_entries|map("\(.key)=\(.value)")|.[]' > envfile
    docker pull --quiet "${docker_image}" 2>&1
    docker run --rm --net=host --env-file=envfile --volume $(pwd)/${artifact_name}:/tmp/${artifact_name} --entrypoint bash "${docker_image}" -c "set -e; cd /tmp/${artifact_name} && if [[ -f ${file_path} ]]; then bash ${file_path}; else echo 'Skipping $1 as ${file_path} does not exist'; fi"
    rm envfile
else
    echo "Skipping the execution of [$1] script"
fi
