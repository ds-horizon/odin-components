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
  AMI_TEMPLATE("ami-template"),
  UNDEPLOY("undeploy"),
  ROLLING_RESTART("rolling-restart"),
  SCALE("scale"),
  PASSIVE_DOWNSCALE("passive-downscale"),
  UPDATE_STACK("update-asg-stack"),
  REDEPLOY("redeploy"),
  REVERT("revert"),
  STATUS("status"),
  UPDATE_ASG("update-asg");
  final String value;

  public static Operations fromValue(String value) {
    return Arrays.stream(Operations.values())
        .filter(operations -> operations.getValue().equals(value))
        .findFirst()
        .orElseThrow(
            () -> new GenericApplicationException(ApplicationError.INVALID_OPERATION, value));
  }
}
