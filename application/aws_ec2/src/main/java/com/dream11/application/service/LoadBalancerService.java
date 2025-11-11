package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.aws.CloudwatchClient;
import com.dream11.application.aws.LoadBalancerClient;
import com.dream11.application.aws.TargetGroupClient;
import com.dream11.application.config.metadata.aws.AwsAccountData;
import com.dream11.application.config.metadata.aws.DiscoveryData;
import com.dream11.application.config.metadata.aws.EC2Data;
import com.dream11.application.config.metadata.aws.NetworkData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.config.user.HealthCheckConfig;
import com.dream11.application.config.user.LoadBalancerConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.Protocol;
import com.dream11.application.entity.CloudWatchMetric;
import com.dream11.application.entity.ProvisionedCapacityResponse;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.state.LoadBalancerState;
import com.dream11.application.state.State;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.elasticloadbalancingv2.endpoints.internal.Arn;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ListenerNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerSchemeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthStateEnum;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class LoadBalancerService {
  @NonNull final LoadBalancerClient loadBalancerClient;
  @NonNull final TargetGroupClient targetGroupClient;
  @NonNull final CloudwatchClient cloudwatchClient;
  @NonNull final NetworkData networkData;
  @NonNull final DeployConfig deployConfig;
  @NonNull final EC2Data ec2Data;
  @NonNull final DiscoveryData discoveryData;
  @NonNull final AwsAccountData awsAccountData;

  Map<String, String> tags;

  @Inject
  private void init() {
    this.tags =
        ApplicationUtil.merge(
            List.of(
                this.awsAccountData.getTags(),
                this.ec2Data.getTags(),
                this.deployConfig.getTags(),
                Constants.COMPONENT_TAGS));
  }

  private Optional<LoadBalancer> getLoadBalancerFromState(
      String identifier, LoadBalancerState lbState, Runnable stateCorrectionFn) {
    // Check if load balancer actually exist
    try {
      LoadBalancer loadBalancer = this.loadBalancerClient.describe(lbState.getArn());
      log.info("Load balancer:[{}] for identifier:[{}] found", lbState.getName(), identifier);
      return Optional.of(loadBalancer);
    } catch (LoadBalancerNotFoundException loadBalancerNotFoundException) {
      log.warn(
          "Load balancer from state:[{}] does not exists. Correcting state...", lbState.getName());
      stateCorrectionFn.run();
    }

    return Optional.empty();
  }

  public void createLoadBalancer(String identifier, Character type) {
    Optional<LoadBalancerState> lbState = Application.getState().getLoadBalancerState(identifier);
    if (lbState.isEmpty()
        || this.getLoadBalancerFromState(
                identifier,
                lbState.get(),
                () -> Application.getState().removeLoadBalancerState(identifier))
            .isEmpty()) {
      // Create new load balancer and add to state
      String id = ApplicationUtil.generateRandomId(Constants.LB_RANDOM_ID_LENGTH);
      LoadBalancer loadBalancer = null;
      if (type.equals(Constants.INTERNAL_IDENTIFIER)) {
        loadBalancer =
            this.loadBalancerClient.create(
                String.format("%s-%s", id, identifier),
                LoadBalancerSchemeEnum.INTERNAL,
                this.deployConfig.getLoadBalancerConfig().getType().getValue(),
                this.networkData.getLbSecurityGroups().getInternal(),
                this.networkData.getLbSubnets().getPrivateSubnets(),
                tags);
      } else if (type.equals(Constants.EXTERNAL_IDENTIFIER)) {
        loadBalancer =
            this.loadBalancerClient.create(
                String.format("%s-%s", id, identifier),
                LoadBalancerSchemeEnum.INTERNET_FACING,
                this.deployConfig.getLoadBalancerConfig().getType().getValue(),
                this.networkData.getLbSecurityGroups().getExternal(),
                this.networkData.getLbSubnets().getPublicSubnets(),
                tags);
      }
      Objects.requireNonNull(loadBalancer);
      log.info(
          "Load balancer:[{}] created for identifier:[{}]",
          loadBalancer.loadBalancerName(),
          identifier);
      Application.getState().addLoadBalancerState(loadBalancer, identifier);
    }
  }

  public void createTargetGroupsAndListeners(String identifier, Character type) {
    Optional<LoadBalancerState> lbState = Application.getState().getLoadBalancerState(identifier);
    if (lbState.isEmpty()) {
      throw new GenericApplicationException(
          ApplicationError.LOAD_BALANCER_DOES_NOT_EXIST, identifier);
    }
    this.deployConfig
        .getLoadBalancerConfig()
        .getListeners()
        .forEach(
            listener ->
                this.createTargetGroupAndListener(identifier, listener, lbState.get(), type));
  }

  private void createTargetGroupAndListener(
      String identifier,
      LoadBalancerConfig.Listener listener,
      LoadBalancerState loadBalancerState,
      Character type) {
    // Create target group
    TargetGroup targetGroup =
        this.createTargetGroup(
            identifier,
            listener.getTargetPort(),
            listener.getTargetProtocol(),
            listener.getHealthChecks(),
            loadBalancerState);
    // Create listener
    this.createListener(listener, targetGroup.targetGroupArn(), loadBalancerState, type);
  }

  private Optional<TargetGroup> getTargetGroupFromState(
      String identifier,
      Integer port,
      Protocol protocol,
      LoadBalancerState.TargetGroupState targetGroupState,
      Runnable stateCorrectionFn) {
    // Check if target group actually exist
    try {
      TargetGroup targetGroup = this.targetGroupClient.describe(targetGroupState.getArn());
      log.info(
          "Target group:[{}] for identifier:[{}] with port:[{}] and protocol:[{}] found",
          targetGroup.targetGroupName(),
          identifier,
          port,
          protocol);
      return Optional.of(targetGroup);
    } catch (TargetGroupNotFoundException targetGroupNotFoundException) {
      log.warn(
          "Target group from state:[{}] does not exists. Correcting state...",
          targetGroupState.getName());
      stateCorrectionFn.run();
    }

    return Optional.empty();
  }

  private TargetGroup createTargetGroup(
      String identifier,
      Integer port,
      Protocol protocol,
      HealthCheckConfig healthCheckConfig,
      LoadBalancerState loadBalancerState) {
    Optional<LoadBalancerState.TargetGroupState> targetGroupState =
        loadBalancerState.getTargetGroupState(port, protocol);
    Optional<TargetGroup> optionalTargetGroup =
        targetGroupState.flatMap(
            state ->
                this.getTargetGroupFromState(
                    identifier,
                    port,
                    protocol,
                    state,
                    () -> loadBalancerState.removeTargetGroupState(state.getArn())));
    if (optionalTargetGroup.isEmpty()) {
      // Create target group
      String id = ApplicationUtil.generateRandomId(Constants.LB_RANDOM_ID_LENGTH);
      TargetGroup targetGroup =
          this.targetGroupClient.create(
              String.format("%s-%s", id, identifier),
              this.networkData.getVpcId(),
              protocol.name(),
              port,
              healthCheckConfig,
              tags);
      loadBalancerState.addTargetGroupState(targetGroup);
      log.info(
          "Target group:[{}] for identifier:[{}] with port:[{}] and protocol:[{}] created",
          targetGroup.targetGroupName(),
          identifier,
          port,
          protocol);
      return targetGroup;
    }
    return optionalTargetGroup.get();
  }

  private Optional<Listener> getListenerFromState(
      String lbName,
      String tgArn,
      LoadBalancerState.ListenerState listenerState,
      Runnable stateCorrectionFn) {
    // Check if listener exist
    try {
      Listener listener = this.loadBalancerClient.describeListener(listenerState.getArn());
      log.info(
          "Listener:[{}] for load balancer:[{}] with target group:[{}] found",
          listenerState.getArn(),
          lbName,
          tgArn);
      return Optional.of(listener);
    } catch (ListenerNotFoundException listenerNotFoundException) {
      log.warn(
          "Listener from state:[{}] does not exists. Correcting state...", listenerState.getArn());
      stateCorrectionFn.run();
    }

    return Optional.empty();
  }

  private void createListener(
      LoadBalancerConfig.Listener listener,
      String tgArn,
      LoadBalancerState loadBalancerState,
      Character type) {

    Optional<LoadBalancerState.ListenerState> listenerState =
        loadBalancerState.getListenerState(listener.getPort(), listener.getProtocol(), tgArn);

    if (listenerState.isEmpty()
        || this.getListenerFromState(
                loadBalancerState.getName(),
                tgArn,
                listenerState.get(),
                () -> loadBalancerState.removeListenerState(listenerState.get().getArn()))
            .isEmpty()) {
      // Create Listener
      List<String> certificateArns = new ArrayList<>();
      if (listener.getProtocol() == Protocol.HTTPS) {
        if (type.equals(Constants.INTERNAL_IDENTIFIER)) {
          certificateArns.add(
              ApplicationUtil.getCertificateArn(
                  this.discoveryData, this.deployConfig.getDiscoveryConfig().getPrivateRoute()));
        } else if (type.equals(Constants.EXTERNAL_IDENTIFIER)) {
          certificateArns.add(
              ApplicationUtil.getCertificateArn(
                  this.discoveryData, this.deployConfig.getDiscoveryConfig().getPublicRoute()));
        }
      }
      Listener createdListener =
          this.loadBalancerClient.createListener(
              loadBalancerState.getArn(),
              tgArn,
              listener.getProtocol().name(),
              listener.getPort(),
              certificateArns);
      log.info(
          "Listener:[{}] for load balancer:[{}] with target group:[{}] created",
          createdListener.listenerArn(),
          loadBalancerState.getName(),
          tgArn);
      loadBalancerState.addListenerState(listener, createdListener.listenerArn(), tgArn);
    }
  }

  public void deleteLoadBalancers() {
    log.info("Deleting application load balancers");
    State state = Application.getState();
    List.copyOf(state.getLb())
        .forEach(
            loadBalancerState -> {
              if (Objects.nonNull(loadBalancerState.getArn())) {
                log.debug("Deleting load balancer:[{}]", loadBalancerState.getName());
                this.loadBalancerClient.delete(loadBalancerState.getArn());
                log.info("Deleted load balancer:[{}]", loadBalancerState.getName());
                state.removeLoadBalancerState(loadBalancerState.getRouteIdentifier());
              }
            });
  }

  public void deleteTargetGroups() {
    log.info("Deleting target groups");
    Application.getState()
        .getLb()
        .forEach(
            loadBalancerState -> {
              if (Objects.nonNull(loadBalancerState.getArn())) {
                List.copyOf(loadBalancerState.getTargetGroups())
                    .forEach(
                        targetGroupState -> {
                          log.debug("Deleting target group:[{}]", targetGroupState.getName());
                          this.targetGroupClient.delete(targetGroupState.getArn());
                          log.info("Deleted target group:[{}]", targetGroupState.getName());
                          loadBalancerState.removeTargetGroupState(targetGroupState.getArn());
                        });
              }
            });
  }

  public void deleteListeners() {
    log.info("Deleting listeners");
    Application.getState()
        .getLb()
        .forEach(
            loadBalancerState -> {
              if (Objects.nonNull(loadBalancerState.getArn())) {
                List.copyOf(loadBalancerState.getListeners())
                    .forEach(
                        listenerState -> {
                          log.debug("Deleting listener:[{}]", listenerState.getArn());
                          this.loadBalancerClient.deleteListener(listenerState.getArn());
                          log.info("Deleted listener:[{}]", listenerState.getArn());
                          loadBalancerState.removeListenerState(listenerState.getArn());
                        });
              }
            });
  }

  public long getHealthyTargets(String targetGroupARN) {
    long healthyTargets =
        this.targetGroupClient.describeTargets(targetGroupARN).stream()
            .filter(
                targetHealthDescription ->
                    targetHealthDescription
                        .targetHealth()
                        .state()
                        .equals(TargetHealthStateEnum.HEALTHY))
            .count();
    log.debug(
        "Number of healthy instances in target group:[{}] is {}", targetGroupARN, healthyTargets);
    return healthyTargets;
  }

  public long getNonDrainingTargets(String targetGroupARN) {
    long targets =
        this.targetGroupClient.describeTargets(targetGroupARN).stream()
            .filter(
                targetHealthDescription ->
                    !targetHealthDescription
                        .targetHealth()
                        .state()
                        .equals(TargetHealthStateEnum.DRAINING))
            .count();
    log.debug(
        "Number of non draining instances in target group:[{}] is {}", targetGroupARN, targets);
    return targets;
  }

  public void scaleLcu(String loadBalancerArn, Integer lcu) {
    this.loadBalancerClient.modifyProvisionedCapacity(loadBalancerArn, lcu);
    log.info("Scaled load balancer:[{}] LCU to [{}]", loadBalancerArn, lcu);
  }

  public ProvisionedCapacityResponse.ProvisionedCapacity getLcu(String loadBalancerArn) {
    ProvisionedCapacityResponse.ProvisionedCapacity capacity =
        this.loadBalancerClient.getProvisionedCapacity(loadBalancerArn);
    log.debug("Provisioned capacity for load balancer:[{}] is:[{}]", loadBalancerArn, capacity);
    return capacity;
  }

  private Double getMetrics(String loadBalancerArn, String metric, Instant startTime) {
    CloudWatchMetric cloudWatchMetric =
        CloudWatchMetric.builder()
            .resourceName("LoadBalancer")
            .resourceValue(this.getLbResourceFromArn(loadBalancerArn))
            .metricName(metric)
            .namespace("AWS/ApplicationELB")
            .build();

    return ApplicationUtil.sumList(
        this.cloudwatchClient
            .getMetric(
                startTime,
                Instant.ofEpochMilli(
                    System.currentTimeMillis()
                        + 1), // Adding 1 to avoid rare condition of start time equals to end time
                cloudWatchMetric)
            .get(0)
            .values());
  }

  public Double getRequestCount(String loadBalancerArn, Instant startTime) {
    Double requestCount = this.getMetrics(loadBalancerArn, "RequestCount", startTime);
    log.debug("Request count:[{}] for load balancer:[{}]", requestCount, loadBalancerArn);
    return requestCount;
  }

  public Double getErrorCount(String loadBalancerArn, Instant startTime) {
    Double errorCount =
        this.getMetrics(loadBalancerArn, "HTTPCode_Target_5XX_Count", startTime)
            + this.getMetrics(loadBalancerArn, "HTTPCode_ELB_5XX_Count", startTime);
    log.debug("Error count:[{}] for load balancer:[{}]", errorCount, loadBalancerArn);
    return errorCount;
  }

  private String getLbResourceFromArn(String arn) {
    Optional<Arn> parsedArn = Arn.parse(arn);
    if (parsedArn.isEmpty()) {
      throw new GenericApplicationException(ApplicationError.INVALID_ARN, arn);
    }
    return String.join(
        "/", parsedArn.get().resource().subList(1, parsedArn.get().resource().size()));
  }

  public List<String> getTargetGroupArnsForStack(String stackId, Character deploymentStack) {
    Optional<LoadBalancerState> internal =
        Application.getState()
            .getLoadBalancerState(
                String.format("%s%s%s", stackId, Constants.INTERNAL_IDENTIFIER, deploymentStack));
    Optional<LoadBalancerState> internetFacing =
        Application.getState()
            .getLoadBalancerState(
                String.format("%s%s%s", stackId, Constants.EXTERNAL_IDENTIFIER, deploymentStack));

    List<String> targetGroupArns = new ArrayList<>();
    internal.ifPresent(
        loadBalancerState ->
            targetGroupArns.addAll(
                loadBalancerState.getTargetGroups().stream()
                    .map(LoadBalancerState.TargetGroupState::getArn)
                    .toList()));
    internetFacing.ifPresent(
        loadBalancerState ->
            targetGroupArns.addAll(
                loadBalancerState.getTargetGroups().stream()
                    .map(LoadBalancerState.TargetGroupState::getArn)
                    .toList()));
    return targetGroupArns;
  }
}
