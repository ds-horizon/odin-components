package com.dream11.redis.operation;

import com.dream11.redis.config.user.UpdateNodeTypeConfig;
import com.dream11.redis.service.RedisService;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))

public class UpdateNodeType implements Operation {
    @NonNull
    final RedisService redisService;
    @NonNull
    final UpdateNodeTypeConfig updateNodeTypeConfig;

    @Override
    public void execute() {
        this.redisService.reconcileState();
        this.redisService.updateNodeType(updateNodeTypeConfig);
    }

}
