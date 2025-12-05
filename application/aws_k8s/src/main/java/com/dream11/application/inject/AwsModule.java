package com.dream11.application.inject;

import com.dream11.application.client.DockerClient;
import com.dream11.application.client.HelmClient;
import com.dream11.application.client.K8sClient;
import com.google.inject.AbstractModule;
import lombok.Builder;

@Builder
public class AwsModule extends AbstractModule {
  private final DockerClient dockerClient;
  private final K8sClient k8sClient;
  private final HelmClient helmClient;

  @Override
  protected void configure() {
    bind(DockerClient.class).toInstance(dockerClient);
    bind(K8sClient.class).toInstance(k8sClient);
    bind(HelmClient.class).toInstance(helmClient);
  }
}
