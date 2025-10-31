package com.dream11.redis.service;

import com.dream11.redis.Application;
import com.dream11.redis.client.RedisClient;
import com.dream11.redis.state.State;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.elasticache.model.ReplicationGroupNotFoundException;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class StateService {

  @NonNull final RedisClient redisClient;

  public void reconcileState() {
    State state = Application.getState();

    if (state.getReplicationGroupIdentifier() != null) {
      try {
        redisClient.getReplicationGroup(state.getReplicationGroupIdentifier());
        log.debug("Found replication group: {}", state.getReplicationGroupIdentifier());
      } catch (ReplicationGroupNotFoundException ex) {
        log.warn(
            "Redis replicationGroup :[{}] from state does not exist. Updating state.",
            state.getReplicationGroupIdentifier());
        state.setReplicationGroupIdentifier(null);
      }
    }
  }
}
