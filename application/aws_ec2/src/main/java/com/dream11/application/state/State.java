package com.dream11.application.state;

import com.dream11.application.Application;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.config.user.LoadBalancerConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class State {
  long version;
  @Builder.Default AMIState image = new AMIState();
  @Builder.Default List<LoadBalancerState> lb = new ArrayList<>();
  @Builder.Default List<LaunchTemplateState> lt = new ArrayList<>();
  @Builder.Default List<AutoscalingGroupState> asg = new ArrayList<>();
  @Builder.Default List<Route53State> r53 = new ArrayList<>();

  DeployConfig deployConfig;

  public void removeLoadBalancerState(String identifier) {
    this.lb.removeIf(
        loadBalancerState -> loadBalancerState.getRouteIdentifier().equals(identifier));
  }

  public void addLoadBalancerState(LoadBalancer loadBalancer, String identifier) {
    this.lb.add(
        LoadBalancerState.builder()
            .name(loadBalancer.loadBalancerName())
            .dns(loadBalancer.dnsName())
            .arn(loadBalancer.loadBalancerArn())
            .routeIdentifier(identifier)
            .build());
  }

  public void addLoadBalancerState(
      LoadBalancerDescription loadBalancer,
      String identifier,
      List<LoadBalancerConfig.Listener> listeners) {
    this.lb.add(
        LoadBalancerState.builder()
            .name(loadBalancer.loadBalancerName())
            .dns(loadBalancer.dnsName())
            .routeIdentifier(identifier)
            .listeners(
                listeners.stream()
                    .map(
                        listener ->
                            LoadBalancerState.ListenerState.builder()
                                .port(listener.getPort())
                                .protocol(listener.getProtocol())
                                .instancePort(listener.getTargetPort())
                                .instanceProtocol(listener.getTargetProtocol())
                                .build())
                    .toList())
            .build());
  }

  public Optional<LoadBalancerState> getLoadBalancerState(String identifier) {
    return this.lb.stream()
        .filter(state -> state.getRouteIdentifier().equals(identifier))
        .findFirst();
  }

  public void addLtState(String id, String architecture) {
    this.lt.add(LaunchTemplateState.builder().id(id).architecture(architecture).build());
  }

  public void removeLtState(String id) {
    this.lt.removeIf(launchTemplateState -> launchTemplateState.getId().equals(id));
  }

  public void addAsgState(AutoScalingGroup asg, List<String> launchTemplateIds) {
    this.asg.add(
        AutoscalingGroupState.builder()
            .name(asg.autoScalingGroupName())
            .ltIds(launchTemplateIds)
            .targetGroupArns(asg.targetGroupARNs())
            .loadBalancerNames(asg.loadBalancerNames())
            .build());
  }

  public Optional<AutoscalingGroupState> getAsgStateFromTgArn(String tgArn) {
    return this.asg.stream()
        .filter(state -> state.getTargetGroupArns().contains(tgArn))
        .findFirst();
  }

  public Optional<AutoscalingGroupState> getAsgStateFromLoadBalancerName(String lbName) {
    return this.asg.stream()
        .filter(state -> state.getLoadBalancerNames().contains(lbName))
        .findFirst();
  }

  public void removeAsgState(String name) {
    this.asg.removeIf(autoscalingGroupState -> autoscalingGroupState.getName().equals(name));
  }

  public void incrementVersion() {
    this.version++;
  }

  public DeployConfig getDeployConfig() {
    // Create deep copy to avoid mutation
    return Objects.isNull(this.deployConfig) ? null : this.deployConfig.deepCopy();
  }

  @JsonIgnore
  public List<AutoscalingGroupState> getAsgStateWithoutTgLb() {
    return this.asg.stream()
        .filter(
            asgState ->
                asgState.getTargetGroupArns().isEmpty()
                    && asgState.getLoadBalancerNames().isEmpty())
        .toList();
  }

  /**
   * Finds load balancer with identifier from state
   *
   * @param identifier blue/green
   * @return pair of string, left element denotes lb arn and right element denotes lb name (for
   *     classic)
   */
  public Pair<String, String> getLbArnOrName(String identifier) {
    Optional<LoadBalancerState> lbState = Application.getState().getLoadBalancerState(identifier);
    if (lbState.isPresent()) {
      if (Objects.nonNull(lbState.get().getArn())) {
        // Application/Network load balancer
        return Pair.of(lbState.get().getArn(), null);
      } else {
        // Classic load balancer
        return Pair.of(null, lbState.get().getName());
      }
    }
    return Pair.of(null, null);
  }

  public void addRoute53State(String hostedZoneId, String route, String identifier) {
    Optional<Route53State> route53StateOpt =
        this.r53.stream().filter(route53State -> route53State.getRoute().equals(route)).findFirst();
    route53StateOpt.ifPresentOrElse(
        route53State -> route53State.getIdentifiers().add(identifier),
        () ->
            this.r53.add(
                Route53State.builder()
                    .route(route)
                    .hostedZoneId(hostedZoneId)
                    .identifiers(new HashSet<>(List.of(identifier)))
                    .build()));
  }

  public void removeRoute53FromState(String route) {
    this.r53.removeIf(route53State -> route53State.getRoute().equals(route));
  }

  public Optional<AutoscalingGroupState> getAsgByName(String name) {
    return this.asg.stream().filter(state -> state.getName().equals(name)).findFirst();
  }
}
