package com.dream11.mysql.exception;

import com.dream11.mysql.error.ErrorCategory;

public class DBInstanceNotFoundException extends RuntimeException {

  public DBInstanceNotFoundException(String name) {
    super(String.format("%s: DB Instance:[%s] does not exists", ErrorCategory.ODIN_ERROR, name));
  }
}
