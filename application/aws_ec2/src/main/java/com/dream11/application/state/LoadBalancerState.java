package com.dream11.application.state;

import com.dream11.application.config.user.LoadBalancerConfig;
import com.dream11.application.constant.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadBalancerState {
  String name;
  String dns;
  String arn;
  String routeIdentifier;
  @Builder.Default List<ListenerState> listeners = new ArrayList<>();
  @Builder.Default List<TargetGroupState> targetGroups = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ListenerState {
    String arn;
    Integer port;
    Protocol protocol;
    Integer instancePort; // Only for classic load balancer
    Protocol instanceProtocol; // Only for classic load balancer
    String tgArn; // Only for application and network load balancer
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TargetGroupState {
    Integer port;
    Protocol protocol;
    String arn; // Only for application and network balancer
    String name;
  }

  public void addListenerState(
      LoadBalancerConfig.Listener listener, String listenerArn, String tgArn) {
    this.listeners.add(
        ListenerState.builder()
            .port(listener.getPort())
            .protocol(listener.getProtocol())
            .arn(listenerArn)
            .tgArn(tgArn)
            .build());
  }

  public Optional<ListenerState> getListenerState(Integer port, Protocol protocol, String tgArn) {
    return this.listeners.stream()
        .filter(
            listener ->
                listener.getPort().equals(port)
                    && listener.getProtocol().equals(protocol)
                    && listener.getTgArn().equals(tgArn))
        .findFirst();
  }

  public Optional<TargetGroupState> getTargetGroupState(Integer port, Protocol protocol) {
    return this.targetGroups.stream()
        .filter(
            targetGroup ->
                targetGroup.getPort().equals(port) && targetGroup.getProtocol().equals(protocol))
        .findFirst();
  }

  public void addTargetGroupState(TargetGroup targetGroup) {
    this.targetGroups.add(
        TargetGroupState.builder()
            .name(targetGroup.targetGroupName())
            .arn(targetGroup.targetGroupArn())
            .port(targetGroup.port())
            .protocol(Protocol.valueOf(targetGroup.protocol().name()))
            .build());
  }

  public void removeTargetGroupState(String tgArn) {
    this.targetGroups.removeIf(targetGroup -> targetGroup.getArn().equals(tgArn));
  }

  public void removeListenerState(String listenerArn) {
    this.listeners.removeIf(listener -> listener.getArn().equals(listenerArn));
  }
}
