package com.dream11.application.inject;

import com.dream11.application.client.DockerClient;
import com.dream11.application.client.HelmClient;
import com.google.inject.AbstractModule;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ClientModule extends AbstractModule {

  @NonNull final HelmClient helmClient;
  @NonNull final DockerClient dockerClient;

  @Override
  protected void configure() {
    bind(HelmClient.class).toInstance(this.helmClient);
    bind(DockerClient.class).toInstance(this.dockerClient);
  }
}
