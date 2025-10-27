package com.dream11.application.aws;

import com.dream11.application.config.user.HealthCheckConfig;
import com.dream11.application.constant.Protocol;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Matcher;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetTypeEnum;

public class TargetGroupClient {

  final ElasticLoadBalancingV2Client loadBalancingV2Client;

  public TargetGroupClient(String region, RetryPolicy retryPolicy) {
    this.loadBalancingV2Client =
        ElasticLoadBalancingV2Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryPolicy(retryPolicy))
            .build();
  }

  public TargetGroup create(
      String name,
      String vpcId,
      String protocol,
      Integer port,
      HealthCheckConfig healthCheckConfig,
      Map<String, String> tags) {
    CreateTargetGroupRequest.Builder createTargetGroupRequestBuilder =
        CreateTargetGroupRequest.builder()
            .name(name)
            .vpcId(vpcId)
            .protocol(ProtocolEnum.valueOf(protocol.equals("GRPC") ? "HTTP" : protocol))
            .port(port)
            .targetType(TargetTypeEnum.INSTANCE)
            .tags(
                tags.entrySet().stream()
                    .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                    .toList())
            .healthCheckEnabled(true)
            .healthCheckPort(port.toString())
            .healthCheckProtocol(protocol.equals("GRPC") ? "HTTP" : protocol)
            .healthCheckIntervalSeconds(healthCheckConfig.getInterval())
            .healthCheckTimeoutSeconds(healthCheckConfig.getTimeout())
            .healthyThresholdCount(healthCheckConfig.getHealthyThreshold())
            .unhealthyThresholdCount(healthCheckConfig.getUnhealthyThreshold());

    if (protocol.equals("GRPC")) {
      createTargetGroupRequestBuilder
          .protocolVersion("GRPC")
          .matcher(Matcher.builder().grpcCode("0").build());
      ;
    }

    if (!protocol.equals(Protocol.TCP.name())) {
      createTargetGroupRequestBuilder.healthCheckPath(healthCheckConfig.getPath());
    }
    return this.loadBalancingV2Client
        .createTargetGroup(createTargetGroupRequestBuilder.build())
        .targetGroups()
        .get(0);
  }

  public List<TargetHealthDescription> describeTargets(String targetGroupArn) {
    return this.loadBalancingV2Client
        .describeTargetHealth(request -> request.targetGroupArn(targetGroupArn))
        .targetHealthDescriptions();
  }

  public void delete(String targetGroupArn) {
    this.loadBalancingV2Client.deleteTargetGroup(request -> request.targetGroupArn(targetGroupArn));
  }

  public TargetGroup describe(String targetGroupArn) {
    return this.loadBalancingV2Client
        .describeTargetGroups(request -> request.targetGroupArns(targetGroupArn))
        .targetGroups()
        .get(0);
  }
}
