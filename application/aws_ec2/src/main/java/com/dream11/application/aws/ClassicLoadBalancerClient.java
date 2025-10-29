package com.dream11.application.aws;

import com.dream11.application.config.user.LoadBalancerConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.Protocol;
import com.dream11.application.entity.ProvisionedCapacityResponse;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.HealthCheck;
import software.amazon.awssdk.services.elasticloadbalancing.model.InstanceState;
import software.amazon.awssdk.services.elasticloadbalancing.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerDescription;
import software.amazon.awssdk.services.elasticloadbalancing.model.Tag;

@Slf4j
public class ClassicLoadBalancerClient {

  final ElasticLoadBalancingClient loadBalancingClient;

  public ClassicLoadBalancerClient(String region, RetryStrategy retryStrategy) {
    this.loadBalancingClient =
        ElasticLoadBalancingClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryStrategy(retryStrategy))
            .build();
  }

  public LoadBalancerDescription create(
      String name,
      String scheme,
      List<String> securityGroups,
      List<String> subnets,
      List<LoadBalancerConfig.Listener> listeners,
      String httpsCertificateArn,
      Map<String, String> tags) {
    CreateLoadBalancerRequest createLoadBalancerRequest =
        CreateLoadBalancerRequest.builder()
            .loadBalancerName(name)
            .scheme(scheme)
            .securityGroups(securityGroups)
            .subnets(subnets)
            .listeners(this.buildClassicLoadBalancerListeners(listeners, httpsCertificateArn))
            .tags(
                tags.entrySet().stream()
                    .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                    .toList())
            .build();
    this.loadBalancingClient.createLoadBalancer(createLoadBalancerRequest);
    LoadBalancerConfig.Listener listenerWithHealthcheck =
        listeners.stream()
            .filter(listener -> Objects.nonNull(listener.getHealthChecks()))
            .findFirst()
            .orElseThrow(
                () ->
                    new GenericApplicationException(ApplicationError.HEALTHCHECK_CONFIG_NOT_FOUND));

    this.configureHealthcheck(name, listenerWithHealthcheck);
    this.enableCrossZoneLoadBalancing(name);
    return this.describe(name);
  }

  public void enableCrossZoneLoadBalancing(String loadBalancerName) {
    this.loadBalancingClient.modifyLoadBalancerAttributes(
        request ->
            request
                .loadBalancerName(loadBalancerName)
                .loadBalancerAttributes(
                    loadBalancerAttributes ->
                        loadBalancerAttributes.crossZoneLoadBalancing(
                            crossZoneLoadBalancing -> crossZoneLoadBalancing.enabled(true))));
  }

  public void configureHealthcheck(String loadBalancerName, LoadBalancerConfig.Listener listener) {
    String healthCheckTargetPath =
        listener.getTargetProtocol() == Protocol.HTTP
                || listener.getTargetProtocol() == Protocol.HTTPS
            ? listener.getHealthChecks().getPath()
            : "";
    HealthCheck healthCheck =
        HealthCheck.builder()
            .healthyThreshold(listener.getHealthChecks().getHealthyThreshold())
            .unhealthyThreshold(listener.getHealthChecks().getUnhealthyThreshold())
            .interval(listener.getHealthChecks().getInterval())
            .timeout(listener.getHealthChecks().getTimeout())
            .target(
                String.format(
                    "%s:%s%s",
                    listener.getTargetProtocol(), listener.getTargetPort(), healthCheckTargetPath))
            .build();
    this.configureHealthcheck(loadBalancerName, healthCheck);
  }

  public void delete(String name) {
    this.loadBalancingClient.deleteLoadBalancer(request -> request.loadBalancerName(name));
  }

  public LoadBalancerDescription describe(String name) {
    return this.loadBalancingClient
        .describeLoadBalancers(request -> request.loadBalancerNames(name))
        .loadBalancerDescriptions()
        .get(0);
  }

  public void configureHealthcheck(String name, HealthCheck healthCheck) {
    this.loadBalancingClient.configureHealthCheck(
        request -> request.loadBalancerName(name).healthCheck(healthCheck));
  }

  private List<software.amazon.awssdk.services.elasticloadbalancing.model.Listener>
      buildClassicLoadBalancerListeners(
          List<LoadBalancerConfig.Listener> listeners, String httpsCertificateArn) {
    return listeners.stream()
        .map(
            listener -> {
              Listener.Builder builder =
                  software.amazon.awssdk.services.elasticloadbalancing.model.Listener.builder()
                      .instancePort(listener.getTargetPort())
                      .instanceProtocol(listener.getTargetProtocol().toString())
                      .loadBalancerPort(listener.getPort())
                      .protocol(listener.getProtocol().toString());

              if (listener.getProtocol() == Protocol.HTTPS) {
                builder.sslCertificateId(httpsCertificateArn);
              }
              return builder.build();
            })
        .toList();
  }

  public List<InstanceState> describeInstanceHealth(String loadBalancerName) {
    return this.loadBalancingClient
        .describeInstanceHealth(request -> request.loadBalancerName(loadBalancerName))
        .instanceStates();
  }

  @SneakyThrows
  public ProvisionedCapacityResponse.ProvisionedCapacity getProvisionedCapacity(
      String loadBalancerName) {
    // LCU feature disabled — return a default provisioned capacity response with 0 LCUs and
    // PROVISIONED status.
    log.info(
        "LCU feature disabled; returning default ProvisionedCapacity for loadBalancerName:{}",
        loadBalancerName);
    return ProvisionedCapacityResponse.ProvisionedCapacity.builder()
        .lcu(0)
        .status(Constants.LCU_PROVISIONED_STATUS)
        .build();
  }

  @SneakyThrows
  public void modifyProvisionedCapacity(String loadBalancerName, Integer lcu) {
    // LCU scaling feature has been disabled — skip making the AWS API call.
    log.info(
        "LCU scaling disabled; skipping ModifyProvisionedCapacity for loadBalancerName:{} and requested LCU:{}",
        loadBalancerName,
        lcu);
  }
}
