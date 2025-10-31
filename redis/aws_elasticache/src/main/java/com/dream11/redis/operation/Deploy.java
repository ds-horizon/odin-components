package com.dream11.redis.operation;

import com.dream11.redis.service.RedisService;
import com.dream11.redis.service.StateService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Deploy implements Operation {
  @NonNull final RedisService redisService;
  @NonNull final StateService stateService;

  @Override
  public void execute() {
    this.stateService.reconcileState();
    this.redisService.deploy();
  }
}
