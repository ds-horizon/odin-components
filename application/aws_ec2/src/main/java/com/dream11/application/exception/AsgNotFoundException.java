package com.dream11.application.exception;

import com.dream11.application.error.ErrorCategory;

public class AsgNotFoundException extends RuntimeException {

  public AsgNotFoundException(String name) {
    super(
        String.format(
            "%s: Autoscaling group:[%s] does not exists", ErrorCategory.ODIN_ERROR, name));
  }
}
