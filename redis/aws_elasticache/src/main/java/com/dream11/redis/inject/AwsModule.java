package com.dream11.redis.inject;

import com.dream11.redis.client.RedisClient;
import com.google.inject.AbstractModule;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class AwsModule extends AbstractModule {
  @NonNull final RedisClient redisClient;

  @Override
  protected void configure() {
    bind(RedisClient.class).toInstance(this.redisClient);
  }
}
