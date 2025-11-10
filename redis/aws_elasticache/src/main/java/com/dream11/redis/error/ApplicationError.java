package com.dream11.redis.error;

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
  SUBNET_GROUP_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No subnet group found"),
  SECURITY_GROUP_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No security group found"),
  CORRUPTED_STATE_FILE(ErrorCategory.ODIN_ERROR, "Corrupted state file"),
  REPLICATION_GROUP_WAIT_TIMEOUT(
      ErrorCategory.AWS_ERROR, "Timeout waiting for Replication Group [%s] to be %s");

  final ErrorCategory category;
  final String message;
}
