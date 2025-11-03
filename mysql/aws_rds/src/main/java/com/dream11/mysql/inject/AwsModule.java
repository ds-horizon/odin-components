package com.dream11.mysql.inject;

import com.dream11.mysql.client.RDSClient;
import com.google.inject.AbstractModule;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class AwsModule extends AbstractModule {
  @NonNull final RDSClient rdsClient;

  @Override
  protected void configure() {
    bind(RDSClient.class).toInstance(this.rdsClient);
  }
}
