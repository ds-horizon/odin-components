package com.dream11.redis.operation;


import com.dream11.redis.service.RedisService;
import com.dream11.redis.service.StateService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class UpdateReplicaCount implements Operation {
    @NonNull final RedisService rdsService;
    @NonNull final StateService stateService;

    @Override
    public void execute() {
        this.stateService.reconcileState();
        this.rdsService.updateReplicaCount();
    }
}