package com.dream11.mysql.exception;

import com.dream11.mysql.error.ErrorCategory;

public class DBClusterNotFoundException extends RuntimeException {

  public DBClusterNotFoundException(String name) {
    super(
        String.format(
            "%s: DB Cluster:[%s] does not exists", ErrorCategory.ODIN_ERROR, name));
  }
}
