package com.dream11.redis.operation;

import com.dream11.redis.config.user.UpdateNodeGroupCountConfig;
import com.dream11.redis.service.RedisService;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class UpdateNodeGroupCount implements Operation {
    @NonNull
    final RedisService redisService;
    @NonNull
    final UpdateNodeGroupCountConfig updateNodeGroupCountConfig;

    @Override
    public void execute() {
        this.redisService.reconcileState();
        this.redisService.updateNodeGroupCount(updateNodeGroupCountConfig);
    }
}
