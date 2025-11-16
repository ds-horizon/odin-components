package com.dream11.redis.operation;

import com.dream11.redis.service.RedisService;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class Deploy implements Operation {
  @NonNull
  final RedisService redisService;

  @Override
  public void execute() {
    this.redisService.reconcileState();
    this.redisService.deploy();
  }
}
