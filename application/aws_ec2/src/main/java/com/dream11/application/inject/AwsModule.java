package com.dream11.application.inject;

import com.dream11.application.aws.AutoscalingGroupClient;
import com.dream11.application.aws.ClassicLoadBalancerClient;
import com.dream11.application.aws.CloudwatchClient;
import com.dream11.application.aws.EC2Client;
import com.dream11.application.aws.LaunchTemplateClient;
import com.dream11.application.aws.LoadBalancerClient;
import com.dream11.application.aws.Route53Client;
import com.dream11.application.aws.SystemsManagerClient;
import com.dream11.application.aws.TargetGroupClient;
import com.google.inject.AbstractModule;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class AwsModule extends AbstractModule {

  @NonNull final LoadBalancerClient loadBalancerClient;
  @NonNull final AutoscalingGroupClient autoscalingGroupClient;
  @NonNull final TargetGroupClient targetGroupClient;
  @NonNull final Route53Client route53Client;
  @NonNull final LaunchTemplateClient launchTemplateClient;
  @NonNull final ClassicLoadBalancerClient classicLoadBalancerClient;
  @NonNull final EC2Client ec2Client;
  @NonNull final CloudwatchClient cloudwatchClient;
  @NonNull final SystemsManagerClient systemsManagerClient;

  @Override
  protected void configure() {
    bind(ClassicLoadBalancerClient.class).toInstance(this.classicLoadBalancerClient);
    bind(LoadBalancerClient.class).toInstance(this.loadBalancerClient);
    bind(TargetGroupClient.class).toInstance(this.targetGroupClient);
    bind(Route53Client.class).toInstance(this.route53Client);
    bind(AutoscalingGroupClient.class).toInstance(this.autoscalingGroupClient);
    bind(LaunchTemplateClient.class).toInstance(this.launchTemplateClient);
    bind(EC2Client.class).toInstance(this.ec2Client);
    bind(CloudwatchClient.class).toInstance(this.cloudwatchClient);
    bind(SystemsManagerClient.class).toInstance(this.systemsManagerClient);
  }
}
