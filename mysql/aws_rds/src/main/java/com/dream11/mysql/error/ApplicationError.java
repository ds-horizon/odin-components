package com.dream11.mysql.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ApplicationError {
  CONSTRAINT_VIOLATION(ErrorCategory.USER_ERROR, "Constraint violation:[%s]"),
  INVALID_ARGUMENTS(
      ErrorCategory.ODIN_ERROR, "Please pass an operation as an argument. Available operations:%s"),
  INVALID_OPERATION(ErrorCategory.ODIN_ERROR, "Invalid operation:[%s]"),
  SERVICE_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No service with category:[%s] found"),
  ACCOUNT_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No account with category:[%s] found"),
  CORRUPTED_STATE_FILE(ErrorCategory.ODIN_ERROR, "Corrupted state file"),
  ODIN_ERROR(ErrorCategory.ODIN_ERROR, "%s"),

  CLUSTER_AVAILABLE_TIMEOUT(
      ErrorCategory.AWS_ERROR, "Timeout waiting for cluster to become available"),
  CLUSTER_NOT_FOUND(ErrorCategory.AWS_ERROR, "Cluster not found: [%s]"),
  CLUSTER_PARAMETER_GROUP_NOT_FOUND(
      ErrorCategory.AWS_ERROR, "Cluster parameter group not found: [%s]"),
  INSTANCE_NOT_FOUND(ErrorCategory.AWS_ERROR, "DB Instance not found: [%s]"),
  INSTANCE_PARAMETER_GROUP_NOT_FOUND(
      ErrorCategory.AWS_ERROR, "DB Instance parameter group not found: [%s]"),
  INSTANCE_AVAILABLE_TIMEOUT(
      ErrorCategory.AWS_ERROR, "Timeout waiting for DB Instance to become available"),
  INSTANCE_DELETE_TIMEOUT(ErrorCategory.AWS_ERROR, "Timeout waiting for DB Instance to be deleted"),
  CLUSTER_DELETE_TIMEOUT(ErrorCategory.AWS_ERROR, "Timeout waiting for cluster to be deleted"),
  INVALID_CREDENTIALS(ErrorCategory.AWS_ERROR, "Invalid credentials");

  final ErrorCategory category;
  final String message;
}
