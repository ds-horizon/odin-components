package com.dream11.redis.operation;

import com.dream11.redis.service.RedisService;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class Undeploy implements Operation {
  @NonNull
  final RedisService redisService;

  @Override
  public void execute() {
    redisService.reconcileState();
    redisService.undeploy();
  }
}
