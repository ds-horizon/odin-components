package com.dream11.application.exception;

import com.dream11.application.error.ApplicationError;

public class GenericApplicationException extends RuntimeException {
  public GenericApplicationException(String message) {
    super(message);
  }

  public GenericApplicationException(ApplicationError applicationError, Object... params) {
    super(
        String.format(
            "%s: %s",
            applicationError.getCategory(),
            String.format(applicationError.getMessage(), params).replaceAll("\\R", ". ")));
  }
}
