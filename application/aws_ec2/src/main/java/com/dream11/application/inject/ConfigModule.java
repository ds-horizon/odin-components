package com.dream11.application.inject;

import com.dream11.application.Application;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.config.metadata.aws.AwsAccountData;
import com.dream11.application.config.metadata.aws.DiscoveryData;
import com.dream11.application.config.metadata.aws.EC2Data;
import com.dream11.application.config.metadata.aws.NetworkData;
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
  NetworkData networkData;
  DiscoveryData discoveryData;
  EC2Data ec2Data;
  AwsAccountData awsAccountData;

  private void init() {
    this.networkData =
        ApplicationUtil.getServiceWithCategory(
            this.componentMetadata.getCloudProviderDetails().getAccount().getServices(),
            Constants.NETWORK_CATEGORY,
            NetworkData.class);
    this.discoveryData =
        ApplicationUtil.getServiceWithCategory(
            this.componentMetadata.getCloudProviderDetails().getAccount().getServices(),
            Constants.DISCOVERY_CATEGORY,
            DiscoveryData.class);
    this.ec2Data =
        ApplicationUtil.getServiceWithCategory(
            this.componentMetadata.getCloudProviderDetails().getAccount().getServices(),
            Constants.EC2_CATEGORY,
            EC2Data.class);
    this.awsAccountData =
        Application.getObjectMapper()
            .convertValue(
                this.componentMetadata.getCloudProviderDetails().getAccount().getData(),
                AwsAccountData.class);
  }

  @Override
  protected void configure() {
    this.init();
    this.validateBindings();
    bind(ComponentMetadata.class).toInstance(this.componentMetadata);
    bind(NetworkData.class).toInstance(this.networkData);
    bind(DiscoveryData.class).toInstance(this.discoveryData);
    bind(EC2Data.class).toInstance(this.ec2Data);
    bind(AwsAccountData.class).toInstance(this.awsAccountData);
    if (Objects.nonNull(this.deployConfig)) {
      bind(DeployConfig.class).toInstance(this.deployConfig);
    }
  }

  private void validateBindings() {
    this.componentMetadata.validate();
    this.awsAccountData.validate();
    this.networkData.validate();
    this.ec2Data.validate();
    this.discoveryData.validate();
    if (Objects.nonNull(this.deployConfig)) {
      this.deployConfig.validate();
    }
  }
}
