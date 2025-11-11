package com.dream11.application.aws;

import com.dream11.application.constant.Constants;
import com.dream11.application.entity.ProvisionedCapacityResponse;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ActionTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Certificate;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerSchemeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupTuple;

@Slf4j
public class LoadBalancerClient {

  final ElasticLoadBalancingV2Client loadBalancingV2Client;

  public LoadBalancerClient(String region, RetryStrategy retryStrategy) {
    this.loadBalancingV2Client =
        ElasticLoadBalancingV2Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryStrategy(retryStrategy))
            .build();
  }

  public LoadBalancer create(
      String name,
      LoadBalancerSchemeEnum scheme,
      String type,
      List<String> securityGroups,
      List<String> subnets,
      Map<String, String> tags) {
    CreateLoadBalancerRequest createLoadBalancerRequest =
        CreateLoadBalancerRequest.builder()
            .name(name)
            .scheme(scheme)
            .securityGroups(securityGroups)
            .subnets(subnets)
            .type(LoadBalancerTypeEnum.fromValue(type))
            .tags(
                tags.entrySet().stream()
                    .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                    .toList())
            .build();
    LoadBalancer loadBalancer =
        this.loadBalancingV2Client
            .createLoadBalancer(createLoadBalancerRequest)
            .loadBalancers()
            .get(0);
    this.enableCrossZoneLoadBalancing(loadBalancer.loadBalancerArn());
    return loadBalancer;
  }

  public void enableCrossZoneLoadBalancing(String loadBalancerArn) {
    this.loadBalancingV2Client.modifyLoadBalancerAttributes(
        request ->
            request
                .loadBalancerArn(loadBalancerArn)
                .attributes(
                    LoadBalancerAttribute.builder()
                        .key("load_balancing.cross_zone.enabled")
                        .value("true")
                        .build()));
  }

  public void delete(String loadBalancerArn) {
    this.loadBalancingV2Client.deleteLoadBalancer(
        request -> request.loadBalancerArn(loadBalancerArn));
  }

  public Listener createListener(
      String loadBalancerArn,
      String targetGroupArn,
      String protocol,
      Integer port,
      List<String> certificateArns) {
    CreateListenerRequest createListenerRequest =
        CreateListenerRequest.builder()
            .loadBalancerArn(loadBalancerArn)
            .protocol(ProtocolEnum.valueOf(protocol))
            .port(port)
            .defaultActions(
                Action.builder()
                    .type(ActionTypeEnum.FORWARD)
                    .targetGroupArn(targetGroupArn)
                    .forwardConfig(
                        forwardConfigBuilder ->
                            forwardConfigBuilder
                                .targetGroups(
                                    TargetGroupTuple.builder()
                                        .targetGroupArn(targetGroupArn)
                                        .build())
                                .build())
                    .build())
            .certificates(
                certificateArns.stream()
                    .map(arn -> Certificate.builder().certificateArn(arn).build())
                    .toList())
            .build();
    return this.loadBalancingV2Client.createListener(createListenerRequest).listeners().get(0);
  }

  public Listener createListener(
      String loadBalancerArn, String targetGroupArn, String protocol, Integer port) {
    return this.createListener(loadBalancerArn, targetGroupArn, protocol, port, List.of());
  }

  public LoadBalancer describe(String loadBalancerArn) {
    return loadBalancingV2Client
        .describeLoadBalancers(request -> request.loadBalancerArns(loadBalancerArn))
        .loadBalancers()
        .get(0);
  }

  public Listener describeListener(String listenerArn) {
    return loadBalancingV2Client
        .describeListeners(request -> request.listenerArns(listenerArn))
        .listeners()
        .get(0);
  }

  @SneakyThrows
  public void modifyProvisionedCapacity(String loadBalancerArn, Integer lcu) {
    // TODO Implement modifyProvisionedCapacity
  }

  @SneakyThrows
  public ProvisionedCapacityResponse.ProvisionedCapacity getProvisionedCapacity(
      String loadBalancerArn) {
    // TODO Implement getProvisionedCapacity
    return ProvisionedCapacityResponse.ProvisionedCapacity.builder()
        .lcu(0)
        .status(Constants.LCU_PROVISIONED_STATUS)
        .build();
  }

  public void deleteListener(String listenerArn) {
    this.loadBalancingV2Client.deleteListener(request -> request.listenerArn(listenerArn));
  }
}
