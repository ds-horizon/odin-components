package com.dream11.application.exception;

public class HelmReleaseNotFoundException extends RuntimeException {
  public HelmReleaseNotFoundException(String message) {
    super(message);
  }

  public HelmReleaseNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
