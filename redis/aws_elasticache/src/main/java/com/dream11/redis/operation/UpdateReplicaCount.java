package com.dream11.redis.operation;

import com.dream11.redis.config.user.UpdateReplicaCountConfig;
import com.dream11.redis.service.RedisService;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class UpdateReplicaCount implements Operation {
    @NonNull
    final RedisService redisService;
    @NonNull
    final UpdateReplicaCountConfig updateReplicaCountConfig;

    @Override
    public void execute() {
        this.redisService.reconcileState();
        this.redisService.updateReplicaCount(updateReplicaCountConfig);
    }
}
