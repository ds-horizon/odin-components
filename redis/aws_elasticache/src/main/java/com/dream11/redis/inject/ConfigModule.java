package com.dream11.redis.inject;

import com.dream11.redis.config.metadata.ComponentMetadata;
import com.dream11.redis.config.metadata.aws.AwsAccountData;
import com.dream11.redis.config.metadata.aws.RedisData;
import com.dream11.redis.config.user.DeployConfig;
import com.google.inject.AbstractModule;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ConfigModule extends AbstractModule {

  @NonNull final ComponentMetadata componentMetadata;
  final DeployConfig deployConfig;
  @NonNull final RedisData redisData;
  @NonNull final AwsAccountData awsAccountData;

  @Override
  protected void configure() {
    this.validateBindings();
    bind(ComponentMetadata.class).toInstance(this.componentMetadata);
    bind(RedisData.class).toInstance(this.redisData);
    bind(AwsAccountData.class).toInstance(this.awsAccountData);
    if (Objects.nonNull(this.deployConfig)) {
      bind(DeployConfig.class).toInstance(this.deployConfig);
    }
  }

  private void validateBindings() {
    this.componentMetadata.validate();
    this.redisData.validate();
    this.awsAccountData.validate();
    if (Objects.nonNull(this.deployConfig)) {
      this.deployConfig.validate();
    }
  }
}
