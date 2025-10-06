package com.dream11.mysql.exception;

import com.dream11.mysql.error.ErrorCategory;

public class DBParameterGroupNotFoundException extends RuntimeException {
  public DBParameterGroupNotFoundException(String name) {
    super(
        String.format(
            "%s: DB Instance Parameter Group:[%s] does not exists",
            ErrorCategory.ODIN_ERROR, name));
  }
}
