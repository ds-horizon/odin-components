package com.dream11.mysql.exception;

import com.dream11.mysql.error.ErrorCategory;

public class DBClusterParameterGroupNotFoundException extends RuntimeException {
  public DBClusterParameterGroupNotFoundException(String name) {
    super(
        String.format(
            "%s: DB Cluster Parameter Group:[%s] does not exists", ErrorCategory.ODIN_ERROR, name));
  }
}
