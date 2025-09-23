package com.dream11.mysql.inject;

import com.dream11.mysql.Application;
import com.dream11.mysql.config.metadata.ComponentMetadata;
import com.dream11.mysql.config.metadata.aws.AwsAccountData;
import com.dream11.mysql.config.metadata.aws.RDSData;
import com.dream11.mysql.config.user.DeployConfig;
import com.dream11.mysql.constant.Constants;
import com.dream11.mysql.util.ApplicationUtil;
import com.google.inject.AbstractModule;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class ConfigModule extends AbstractModule {

  @NonNull final ComponentMetadata componentMetadata;
  final DeployConfig deployConfig;
  RDSData rdsData;
  AwsAccountData awsAccountData;

  private void init() {
    this.rdsData =
        ApplicationUtil.getServiceWithCategory(
            this.componentMetadata.getCloudProviderDetails().getAccount().getServices(),
            Constants.RDS_CATEGORY,
            RDSData.class);
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
    bind(RDSData.class).toInstance(this.rdsData);
    bind(AwsAccountData.class).toInstance(this.awsAccountData);
    if (Objects.nonNull(this.deployConfig)) {
      bind(DeployConfig.class).toInstance(this.deployConfig);
    }
  }

  private void validateBindings() {
    this.componentMetadata.validate();
    this.rdsData.validate();
    this.awsAccountData.validate();
    if (Objects.nonNull(this.deployConfig)) {
      this.deployConfig.validate();
    }
  }
}
