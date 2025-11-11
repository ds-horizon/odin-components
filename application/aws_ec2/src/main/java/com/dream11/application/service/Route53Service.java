package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.aws.Route53Client;
import com.dream11.application.config.metadata.aws.DiscoveryData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.entity.Route53Record;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.exception.Route53NotFoundException;
import com.dream11.application.state.LoadBalancerState;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Route53Service {

  @NonNull final Route53Client route53Client;

  @NonNull final DiscoveryData discoveryData;
  @NonNull final DeployConfig deployConfig;

  public void createRoute53(String identifier, Character type) {
    Optional<LoadBalancerState> lbState = Application.getState().getLoadBalancerState(identifier);
    if (lbState.isEmpty()) {
      throw new GenericApplicationException(
          ApplicationError.LOAD_BALANCER_DOES_NOT_EXIST, identifier);
    }
    String hostedZoneId = null;
    String route = null;
    if (type.equals(Constants.INTERNAL_IDENTIFIER)) {
      route = this.deployConfig.getDiscoveryConfig().getPrivateRoute();
      hostedZoneId = this.discoveryData.getDomainFromRoute(route).getId();
    } else if (type.equals(Constants.EXTERNAL_IDENTIFIER)) {
      route = this.deployConfig.getDiscoveryConfig().getPublicRoute();
      hostedZoneId = this.discoveryData.getDomainFromRoute(route).getId();
    }
    this.createRoute53(identifier, hostedZoneId, route, lbState.get());
  }

  private void createRoute53(
      String identifier, String hostedZoneId, String route, LoadBalancerState lbState) {
    Route53Record r53Record;
    try {
      r53Record = this.route53Client.get(hostedZoneId, route, identifier);
      if (r53Record.getDns().equals(lbState.getDns())) {
        log.info("Route53:[{}] for identifier:[{}] found", route, identifier);
        return;
      } else {
        if (r53Record.getWeight() != 0L) {
          throw new GenericApplicationException(
              ApplicationError.INCORRECT_ACTIVE_ROUTE_FOUND, route, identifier);
        }
        log.warn(
            "Route53 with name:[{}] and identifier:[{}] does not have correct dns value. Correcting...",
            route,
            identifier);
        r53Record.setDns(lbState.getDns());
      }
    } catch (Route53NotFoundException route53NotFoundException) {
      log.warn(
          "Route53 with name:[{}] and identifier:[{}] not found. Creating...", route, identifier);
      r53Record =
          Route53Record.builder()
              .name(route)
              .dns(lbState.getDns())
              .ttl(Constants.ROUTE_53_TTL)
              .weight(0L)
              .identifier(identifier)
              .build();
    }
    this.route53Client.createOrUpdate(hostedZoneId, List.of(r53Record));
    Application.getState().addRoute53State(hostedZoneId, route, identifier);
    log.info("Route53:[{}] for identifier:[{}] created", route, identifier);
  }

  public void deleteRoute53s() {
    log.info("Deleting route53s");
    List.copyOf(Application.getState().getR53())
        .forEach(
            route53State -> {
              log.debug(
                  "Deleting route:[{}] with identifiers:[{}]",
                  route53State.getRoute(),
                  route53State.getIdentifiers());
              this.route53Client.delete(
                  route53State.getHostedZoneId(),
                  route53State.getRoute(),
                  route53State.getIdentifiers().stream().toList());
              Application.getState().removeRoute53FromState(route53State.getRoute());
              log.info(
                  "Deleted route:[{}] with identifiers:[{}]",
                  route53State.getRoute(),
                  route53State.getIdentifiers());
            });
  }

  /**
   * Fetch weights of all route53 records created
   *
   * @return route53weights Map containing weights for identifiers. Key: identifier, Value: weight
   */
  public Map<String, Long> getRoute53Weights() {
    List<String> privateIdentifiers =
        ApplicationUtil.getPrivateIdentifiers(
            this.deployConfig.getStacks(), this.deployConfig.getDiscoveryConfig().getType());

    List<String> publicIdentifiers =
        ApplicationUtil.getPublicIdentifiers(
            this.deployConfig.getStacks(), this.deployConfig.getDiscoveryConfig().getType());

    Map<String, Long> weights = new HashMap<>();

    if (!privateIdentifiers.isEmpty()) {
      String privateRoute = this.deployConfig.getDiscoveryConfig().getPrivateRoute();
      weights.putAll(
          this.getRoute53Weights(
              this.discoveryData.getDomainFromRoute(privateRoute).getId(),
              privateRoute,
              privateIdentifiers));
    }
    if (!publicIdentifiers.isEmpty()) {
      String publicRoute = this.deployConfig.getDiscoveryConfig().getPublicRoute();
      weights.putAll(
          this.getRoute53Weights(
              this.discoveryData.getDomainFromRoute(publicRoute).getId(),
              publicRoute,
              publicIdentifiers));
    }
    return weights;
  }

  /**
   * Converts weight map into expanded weight map
   *
   * @return Map<String, Map < Character, Map < Character, Long>>> expanded weight map. Example: {0
   *     : {i : {g : 100, b : 0}, {e : {g : 100, b : 0}}}
   */
  public Map<String, Map<Character, Map<Character, Long>>> getExpandedRoute53Weights() {
    Map<String, Long> weightMap = this.getRoute53Weights();
    Map<String, Map<Character, Map<Character, Long>>> expandedWeightkMap = new HashMap<>();
    weightMap.forEach(
        (key, value) ->
            expandedWeightkMap
                .computeIfAbsent(
                    key.substring(0, key.length() - 2),
                    k -> new HashMap<>()) // stack id is till second last character
                .computeIfAbsent(
                    key.charAt(key.length() - 2),
                    k -> new HashMap<>()) // key.length() - 2 contains type i.e i/e
                .put(
                    key.charAt(key.length() - 1),
                    value)); // key.length() - 2 contains deployment stack i.e b/g
    return expandedWeightkMap;
  }

  public Map<String, Map<Character, Map<Character, Long>>> getFilteredRoute53Weights(
      List<String> stackIds) {
    if (Objects.nonNull(stackIds)) {
      return this.getExpandedRoute53Weights().entrySet().stream()
          .filter(entry -> stackIds.contains(entry.getKey()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    return this.getExpandedRoute53Weights();
  }

  /**
   * Fetch weights of all route53 records created
   *
   * @param hostedZone id of the hosted zone where the record is present
   * @param route name of the route
   * @param identifiers list of identifiers for the route
   * @return route53weights Map containing weights for identifiers. Key: identifier, Value: weight
   */
  private Map<String, Long> getRoute53Weights(
      String hostedZone, String route, List<String> identifiers) {
    return identifiers.stream()
        .map(
            identifier ->
                Map.entry(
                    identifier, this.route53Client.get(hostedZone, route, identifier).getWeight()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * @param stackId stack id
   * @param weights map containing weights for each deployment stack. Example {b -> 0, g- > 100}
   */
  public void setWeights(String stackId, Map<Character, Long> weights) {
    log.info(
        "Updating weights of route53 records for stack:[{}] with distribution:[{}]",
        stackId,
        weights);
    List<String> changeIds = new ArrayList<>();
    if (this.deployConfig.getDiscoveryConfig().getType() == DiscoveryType.PRIVATE
        || this.deployConfig.getDiscoveryConfig().getType() == DiscoveryType.BOTH) {
      String route = this.deployConfig.getDiscoveryConfig().getPrivateRoute();
      changeIds.add(
          this.route53Client.updateWeights(
              this.discoveryData.getDomainFromRoute(route).getId(),
              route,
              this.generateWeightMap(stackId, Constants.INTERNAL_IDENTIFIER, weights)));
    }
    if (this.deployConfig.getDiscoveryConfig().getType() == DiscoveryType.PUBLIC
        || this.deployConfig.getDiscoveryConfig().getType() == DiscoveryType.BOTH) {
      String route = this.deployConfig.getDiscoveryConfig().getPublicRoute();
      changeIds.add(
          this.route53Client.updateWeights(
              this.discoveryData.getDomainFromRoute(route).getId(),
              route,
              this.generateWeightMap(stackId, Constants.EXTERNAL_IDENTIFIER, weights)));
    }
    List<Callable<Boolean>> waitForChangesToSync =
        changeIds.stream()
            .map(changeId -> (Callable<Boolean>) () -> this.waitForChangeToSync(changeId))
            .toList();
    ApplicationUtil.runOnExecutorService(waitForChangesToSync);
  }

  @SneakyThrows
  private boolean waitForChangeToSync(String changeId) {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() <= startTime + Constants.WAIT_FOR_R53_SYNC.toMillis()) {
      String status = this.route53Client.getChange(changeId);
      log.debug("Change status for changeId {}: {}", changeId, status);
      if ("INSYNC".equals(status)) {
        return true;
      }

      // sleep required to avoid API throttling from Route 53
      Thread.sleep(Constants.DELAY_FOR_MAKING_NEXT_REQUEST.toMillis());
    }
    throw new GenericApplicationException(
        ApplicationError.R53_SYNC, Constants.WAIT_FOR_R53_SYNC.toMinutes());
  }

  private Map<String, Long> generateWeightMap(
      String stackId, Character type, Map<Character, Long> weights) {
    return weights.entrySet().stream()
        .collect(Collectors.toMap(entry -> stackId + type + entry.getKey(), Map.Entry::getValue));
  }
}
