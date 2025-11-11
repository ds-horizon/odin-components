package com.dream11.application.exception;

import com.dream11.application.error.ErrorCategory;

public class LaunchTemplateNotFoundException extends RuntimeException {
  public LaunchTemplateNotFoundException(String id) {
    super(String.format("%s: Launch template:[%s] does not exists", ErrorCategory.ODIN_ERROR, id));
  }
}
