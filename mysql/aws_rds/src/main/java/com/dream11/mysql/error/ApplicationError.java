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
  DB_WAIT_TIMEOUT(ErrorCategory.AWS_ERROR, "Timeout waiting for DB %s [%s] to be %s"),
  CANNOT_MODIFY_PARAMETER_GROUP_CONFIG(
      ErrorCategory.USER_ERROR,
      "Once used the parameter group name you cannot change the parameter group config. You can only provide another parameter group name");

  final ErrorCategory category;
  final String message;
}
