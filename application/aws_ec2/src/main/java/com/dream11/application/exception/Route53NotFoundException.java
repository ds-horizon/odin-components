package com.dream11.application.exception;

public class Route53NotFoundException extends RuntimeException {

  public Route53NotFoundException(String message) {
    super(message);
  }
}
