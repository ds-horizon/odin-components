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
      "Local container flavour cannot be invoked with %s cloud provider. Requires 'local' cloud provider"),
  DOCKER_IMAGE_FETCH_FAILED(
      ErrorCategory.DOCKER_ERROR, "Fetching docker image failed with status code: [%s]"),
  HELM_CHART_UPGRADE_FAILED(ErrorCategory.HELM_ERROR, "Failed to install helm chart. Error:[%s]"),
  HELM_CHART_UNINSTALL_FAILED(
      ErrorCategory.HELM_ERROR, "Failed to uninstall helm chart. Error:[%s]"),
  CORRUPTED_STATE_FILE(ErrorCategory.ODIN_ERROR, "Corrupted state file"),
  ACCOUNT_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No account with category:[%s] found"),
  INVALID_OPERATION(ErrorCategory.ODIN_ERROR, "Invalid operation:[%s]"),
  HELM_TEMPLATE_FILE_NOT_FOUND(
      ErrorCategory.ODIN_ERROR, "Helm values template file:[%s] not found"),
  PACKER_TEMPLATE_FILE_NOT_FOUND(ErrorCategory.ODIN_ERROR, "Packer template file:[%s] not found"),
  NO_DOCKER_REGISTRY_FOR_PUBLISHING(
      ErrorCategory.USER_ERROR, "No docker registry found with push enabled"),
  MULTIPLE_DOCKER_REGISTRY_FOR_PUBLISHING(
      ErrorCategory.USER_ERROR, "More than one docker registry found with push enabled");
  final ErrorCategory category;
  final String message;
}
