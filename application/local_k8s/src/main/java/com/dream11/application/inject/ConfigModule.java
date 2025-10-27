package com.dream11.application.inject;

import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.config.metadata.local.DockerRegistryData;
import com.dream11.application.config.metadata.local.KubernetesData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.AbstractModule;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ConfigModule extends AbstractModule {

  @NonNull final ComponentMetadata componentMetadata;
  final DeployConfig deployConfig;
  final DockerRegistryData dockerRegistryData;
  KubernetesData kubernetesData;

  private void init() {
    this.kubernetesData =
        ApplicationUtil.getServicesWithCategory(
                this.componentMetadata, Constants.KUBERNETES_CATEGORY, KubernetesData.class)
            .get(0);
  }

  @Override
  protected void configure() {
    this.init();
    this.validateBindings();
    bind(ComponentMetadata.class).toInstance(this.componentMetadata);
    bind(DockerRegistryData.class).toInstance(this.dockerRegistryData);
    bind(KubernetesData.class).toInstance(this.kubernetesData);
    if (Objects.nonNull(this.deployConfig)) {
      bind(DeployConfig.class).toInstance(this.deployConfig);
    }
  }

  private void validateBindings() {
    this.componentMetadata.validate();
    this.dockerRegistryData.validate();
    if (Objects.nonNull(this.deployConfig)) {
      this.deployConfig.validate();
    }
  }
}
