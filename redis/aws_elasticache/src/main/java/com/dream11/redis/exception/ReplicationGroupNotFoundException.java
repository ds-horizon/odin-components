package com.dream11.redis.exception;

import com.dream11.redis.error.ErrorCategory;

public class ReplicationGroupNotFoundException extends RuntimeException {

  public ReplicationGroupNotFoundException(String name) {
    super(
        String.format(
            "%s: Replication Group:[%s] does not exists", ErrorCategory.ODIN_ERROR, name));
  }
}
