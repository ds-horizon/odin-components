package com.dream11.application.exception;

import com.dream11.application.error.ErrorCategory;

public class HelmReleaseNotFoundException extends RuntimeException {

  final String releaseName;

  public HelmReleaseNotFoundException(String message, String releaseName) {
    super(ErrorCategory.ODIN_ERROR + ": " + message.replaceAll("\\R", ". "));
    this.releaseName = releaseName;
  }
}
