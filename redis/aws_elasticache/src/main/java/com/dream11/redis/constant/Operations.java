package com.dream11.redis.constant;

import java.util.Arrays;

import com.dream11.redis.error.ApplicationError;
import com.dream11.redis.exception.GenericApplicationException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Operations {
  DEPLOY("deploy"),
  UNDEPLOY("undeploy"),
  UPDATE_NODE_TYPE("update-node-type"),
  UPDATE_REPLICA_COUNT("update-replica-count"),
  UPDATE_NODE_GROUP_COUNT("update-nodegroup-count");

  final String value;

  public static Operations fromValue(String value) {
    return Arrays.stream(Operations.values())
        .filter(operations -> operations.getValue().equals(value))
        .findFirst()
        .orElseThrow(
            () -> new GenericApplicationException(ApplicationError.INVALID_OPERATION, value));
  }
}
