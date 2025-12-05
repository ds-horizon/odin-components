package com.dream11.application.inject;

import com.dream11.application.config.ComponentMetadata;
import com.dream11.application.config.DeployConfig;
import com.dream11.application.config.aws.AwsAccountData;
import com.dream11.application.config.aws.DockerRegistryData;
import com.dream11.application.config.aws.EKSData;
import com.dream11.application.config.aws.VPCData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ConfigModule extends AbstractModule {

  @NonNull final ComponentMetadata componentMetadata;
  final DeployConfig deployConfig;
  @NonNull final AwsAccountData awsAccountData;
  @NonNull final DockerRegistryData dockerRegistryData;
  @NonNull final EKSData eksData;
  @NonNull final VPCData vpcData;
  @NonNull final ObjectMapper objectMapper;

  @Override
  protected void configure() {
    bind(ComponentMetadata.class).toInstance(this.componentMetadata);
    bind(AwsAccountData.class).toInstance(this.awsAccountData);
    bind(DockerRegistryData.class).toInstance(this.dockerRegistryData);
    bind(EKSData.class).toInstance(this.eksData);
    bind(VPCData.class).toInstance(this.vpcData);
    bind(ObjectMapper.class).toInstance(this.objectMapper);
    if (Objects.nonNull(this.deployConfig)) {
      bind(DeployConfig.class).toInstance(this.deployConfig);
    }
  }
}
