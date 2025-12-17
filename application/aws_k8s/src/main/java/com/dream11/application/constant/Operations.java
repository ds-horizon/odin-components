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
  REDEPLOY("redeploy"),
  IMAGE_TEMPLATE("image-template"),
  CREATE_NAMESPACE("create-namespace"),
  UNDEPLOY("undeploy"),
  SCALE("scale"),
  ROLLING_RESTART("rolling-restart"),
  REVERT("revert"),
  STATUS("status");
  final String value;

  public static Operations fromValue(String value) {
    return Arrays.stream(Operations.values())
        .filter(operations -> operations.getValue().equals(value))
        .findFirst()
        .orElseThrow(
            () -> new GenericApplicationException(ApplicationError.INVALID_OPERATION, value));
  }
}
