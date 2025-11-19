package com.dream11.application.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ApplicationError {
  CONSTRAINT_VIOLATION(ErrorCategory.USER_ERROR, "Constraint violation:[%s]"),
  INVALID_ARGUMENTS(
      ErrorCategory.ODIN_ERROR, "Please pass an operation as an argument. Available operations:%s"),
  DEPLOY_CONFIG_NOT_FOUND_IN_STATE(
      ErrorCategory.ODIN_ERROR, "Deploy config not found in state. Cannot perform %s operation"),
  INVALID_CLOUD_PROVIDER(
      ErrorCategory.ODIN_ERROR,
      "AWS container v2 flavour cannot be invoked with %s cloud provider. Requires 'aws' cloud provider"),
  DOCKER_IMAGE_FETCH_FAILED(
      ErrorCategory.DOCKER_ERROR, "Fetching docker image failed with status code: [%s]"),
  SERVICE_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No service with category:[%s] found"),
  ACCOUNT_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No account with category:[%s] found"),
  INVALID_OPERATION(ErrorCategory.ODIN_ERROR, "Invalid operation:[%s]"),
  INVALID_CONFIG(ErrorCategory.ODIN_ERROR, "Invalid config for:[%s]"),
  PACKER_TEMPLATE_FILE_NOT_FOUND(ErrorCategory.ODIN_ERROR, "Packer template file:[%s] not found"),
  HELM_CHART_INSTALL_FAILED(ErrorCategory.ODIN_ERROR, "Helm chart installation failed: %s"),
  HELM_CHART_UPGRADE_FAILED(ErrorCategory.ODIN_ERROR, "Helm chart upgrade failed: %s"),
  HELM_DEPENDENCY_BUILD_FAILED(ErrorCategory.ODIN_ERROR, "Helm dependency build failed: %s"),
  HELM_TEMPLATE_FILE_NOT_FOUND(
      ErrorCategory.ODIN_ERROR, "Helm values template file:[%s] not found"),
  HELM_CHART_UNINSTALL_FAILED(ErrorCategory.ODIN_ERROR, "Helm chart uninstall failed: %s"),
  HELM_ROLLBACK_FAILED(ErrorCategory.ODIN_ERROR, "Helm rollback failed: %s"),
  NO_ACTIVE_DEPLOYMENT_FOUND(ErrorCategory.ODIN_ERROR, "No active deployment found"),
  NO_DOCKER_REGISTRY_FOR_PUBLISHING(
      ErrorCategory.USER_ERROR, "No docker registry found with push enabled"),
  MULTIPLE_DOCKER_REGISTRY_FOR_PUBLISHING(
      ErrorCategory.USER_ERROR, "More than one docker registry found with push enabled"),
  HEALTHY_PODS_COUNT_LESS_THAN_ONE(
      ErrorCategory.ODIN_ERROR, "Healthy pods count is less than one for deployment:[%s]"),
  GATEWAY_CHART_VALUES_PATH_NOT_FOUND(
      ErrorCategory.ODIN_ERROR, "Gateway chart path not found at location:[%s]");

  final ErrorCategory category;
  final String message;
}
