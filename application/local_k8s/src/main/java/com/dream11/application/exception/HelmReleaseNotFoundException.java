package com.dream11.application.exception;

import com.dream11.application.error.ErrorCategory;

public class HelmReleaseNotFoundException extends RuntimeException {

  public HelmReleaseNotFoundException(String message) {
    super(ErrorCategory.ODIN_ERROR + ": " + message.replaceAll("\\R", ". "));
  }
}
