package com.dream11.mysql.constant;

import com.dream11.mysql.error.ApplicationError;
import com.dream11.mysql.exception.GenericApplicationException;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Operations {
  DEPLOY("deploy"),
  ADD_READERS("add-readers"),
  REMOVE_READERS("remove-readers"),
  FAILOVER("failover"),
  REBOOT("reboot"),
  UNDEPLOY("undeploy");
  final String value;

  public static Operations fromValue(String value) {
    return Arrays.stream(Operations.values())
        .filter(operations -> operations.getValue().equals(value))
        .findFirst()
        .orElseThrow(
            () -> new GenericApplicationException(ApplicationError.INVALID_OPERATION, value));
  }
}
