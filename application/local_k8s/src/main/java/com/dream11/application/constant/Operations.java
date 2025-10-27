package com.dream11.application.constant;

import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Operations {
  DEPLOY("deploy"),
  UNDEPLOY("undeploy"),
  IMAGE_TEMPLATE("image-template"),
  REDEPLOY("redeploy");
  final String value;

  public static Operations fromValue(String value) {
    return Arrays.stream(Operations.values())
        .filter(operations -> operations.getValue().equals(value))
        .findFirst()
        .orElseThrow(
            () -> new GenericApplicationException(ApplicationError.INVALID_OPERATION, value));
  }
}
