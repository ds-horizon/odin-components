package com.dream11.redis.exception;

import com.dream11.redis.error.ApplicationError;

public class GenericApplicationException extends RuntimeException {

  public GenericApplicationException(ApplicationError applicationError, Object... params) {
    super(
        String.format(
            "%s: %s",
            applicationError.getCategory(), String.format(applicationError.getMessage(), params)));
  }
}
