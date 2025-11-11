package com.dream11.application.aws;

import com.dream11.application.Application;
import com.dream11.application.config.user.AutoScalingGroupConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.exception.AsgNotFoundException;
import com.dream11.application.state.LaunchTemplateState;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.CreateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.EnableMetricsCollectionRequest;
import software.amazon.awssdk.services.autoscaling.model.InstanceMaintenancePolicy;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateOverrides;
import software.amazon.awssdk.services.autoscaling.model.MixedInstancesPolicy;
import software.amazon.awssdk.services.autoscaling.model.Tag;

@Slf4j
public class AutoscalingGroupClient {

  final AutoScalingClient autoScalingClient;

  public AutoscalingGroupClient(String region, RetryStrategy retryStrategy) {
    this.autoScalingClient =
        AutoScalingClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryStrategy(retryStrategy))
            .build();
  }

  public AutoScalingGroup create(
      String name,
      List<String> targetGroupArns,
      List<String> loadBalancerNames,
      Map<String, String> launchTemplateIdArchitectureMap,
      List<String> subnets,
      AutoScalingGroupConfig autoScalingGroupConfig,
      Map<String, String> tags) {
    Map<String, List<String>> launchTemplateToInstanceTypesMap =
        launchTemplateIdArchitectureMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry ->
                        autoScalingGroupConfig.getInstances().stream()
                            .filter(instance -> instance.getArchitecture().equals(entry.getValue()))
                            .flatMap(instance -> instance.getTypes().stream())
                            .toList()));
    Integer desiredInstances = autoScalingGroupConfig.getDesiredInstances();
    Integer maxSize = autoScalingGroupConfig.getMaxInstances();
    CreateAutoScalingGroupRequest createAutoScalingGroupRequest =
        CreateAutoScalingGroupRequest.builder()
            .autoScalingGroupName(name)
            .targetGroupARNs(targetGroupArns)
            .loadBalancerNames(loadBalancerNames)
            .vpcZoneIdentifier(String.join(",", subnets))
            .healthCheckType("ELB")
            .terminationPolicies(autoScalingGroupConfig.getTerminationPolicies())
            .capacityRebalance(autoScalingGroupConfig.getCapacityRebalance())
            .desiredCapacity(autoScalingGroupConfig.getInitialCapacity())
            .maxSize(desiredInstances > maxSize ? desiredInstances : maxSize)
            .minSize(0)
            .defaultCooldown(autoScalingGroupConfig.getDefaultCooldown())
            .defaultInstanceWarmup(autoScalingGroupConfig.getDefaultInstanceWarmup())
            .instanceMaintenancePolicy(
                policy -> buildInstanceMaintenancePolicy(policy, autoScalingGroupConfig).build())
            .healthCheckGracePeriod(autoScalingGroupConfig.getHealthcheckGracePeriod())
            .mixedInstancesPolicy(
                mixedInstancePolicyBuilder ->
                    buildMixedInstancesPolicy(
                            mixedInstancePolicyBuilder,
                            launchTemplateToInstanceTypesMap,
                            autoScalingGroupConfig)
                        .build())
            .tags(
                tags.entrySet().stream()
                    .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                    .toList())
            .build();
    this.autoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
    EnableMetricsCollectionRequest collectionRequest =
        EnableMetricsCollectionRequest.builder()
            .autoScalingGroupName(name)
            .granularity("1Minute")
            .build();

    this.autoScalingClient.enableMetricsCollection(collectionRequest);

    // Wait till autoscaling group is created
    WaiterResponse<DescribeAutoScalingGroupsResponse> waiterResponse =
        autoScalingClient
            .waiter()
            .waitUntilGroupExists(request -> request.autoScalingGroupNames(name));
    DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResponse =
        waiterResponse
            .matched()
            .response()
            .orElseThrow(
                () ->
                    AutoScalingException.builder()
                        .message(String.format("ASG:[%s] could not be created", name))
                        .build());

    if (!autoScalingGroupConfig.getSuspendProcesses().isEmpty()) {
      this.autoScalingClient.suspendProcesses(
          request ->
              request
                  .autoScalingGroupName(name)
                  .scalingProcesses(autoScalingGroupConfig.getSuspendProcesses()));
    }
    if (Objects.nonNull(autoScalingGroupConfig.getSnsTopicArn())) {
      this.createNotification(name, autoScalingGroupConfig.getSnsTopicArn());
    }
    return describeAutoScalingGroupsResponse.autoScalingGroups().get(0);
  }

  public void createNotification(String name, String snsTopicArn) {
    this.autoScalingClient.putNotificationConfiguration(
        request ->
            request
                .autoScalingGroupName(name)
                .notificationTypes(Constants.ASG_NOTIFICATION_TYPES)
                .topicARN(snsTopicArn));
  }

  public void delete(String name) {
    this.autoScalingClient.deleteAutoScalingGroup(
        request -> request.autoScalingGroupName(name).forceDelete(true));
  }

  public void setDesiredCapacity(String name, Integer desiredInstances) {
    this.autoScalingClient.updateAutoScalingGroup(
        request ->
            request
                .autoScalingGroupName(name)
                .desiredCapacity(desiredInstances)
                .minSize(desiredInstances));
  }

  public void setDesiredCapacity(String name, Integer desiredInstances, Integer maxSize) {
    this.autoScalingClient.updateAutoScalingGroup(
        request ->
            request
                .autoScalingGroupName(name)
                .desiredCapacity(desiredInstances)
                .maxSize(desiredInstances > maxSize ? desiredInstances : maxSize)
                .minSize(desiredInstances));
  }

  public AutoScalingGroup describe(String name) {
    List<AutoScalingGroup> autoScalingGroups =
        this.autoScalingClient
            .describeAutoScalingGroups(request -> request.autoScalingGroupNames(name))
            .autoScalingGroups();
    if (autoScalingGroups.isEmpty()) {
      throw new AsgNotFoundException(name);
    }
    return autoScalingGroups.get(0);
  }

  public void detachTargetGroups(String name, List<String> targetGroupARNs) {
    this.autoScalingClient.detachLoadBalancerTargetGroups(
        request -> request.autoScalingGroupName(name).targetGroupARNs(targetGroupARNs));
  }

  public void detachLoadBalancers(String name, List<String> loadBalancerNames) {
    this.autoScalingClient.detachLoadBalancers(
        request -> request.autoScalingGroupName(name).loadBalancerNames(loadBalancerNames));
  }

  public void updateTag(String name, Map<String, String> tags) {
    this.autoScalingClient.createOrUpdateTags(
        request ->
            request.tags(
                tags.entrySet().stream()
                    .map(
                        entry ->
                            Tag.builder()
                                .key(entry.getKey())
                                .value(entry.getValue())
                                .resourceId(name)
                                .propagateAtLaunch(true)
                                .resourceType("auto-scaling-group")
                                .build())
                    .toList()));
  }

  public void updateAsg(String name, AutoScalingGroupConfig autoScalingGroupConfig) {
    // Fetch LT ids for the given ASG before creating launchTemplateToInstanceTypesMap
    Set<String> ltIdsInAsg =
        Application.getState().getAsg().stream()
            .filter(asg -> asg.getName().equals(name))
            .flatMap(asg -> asg.getLtIds().stream())
            .collect(Collectors.toSet());
    Map<String, List<String>> launchTemplateToInstanceTypesMap =
        Application.getState().getLt().stream()
            .filter(ltEntry -> ltIdsInAsg.contains(ltEntry.getId()))
            .collect(
                Collectors.toMap(
                    LaunchTemplateState::getId,
                    ltEntry -> {
                      String arch = ltEntry.getArchitecture();
                      return autoScalingGroupConfig.getInstances().stream()
                          .filter(inst -> arch.equals(inst.getArchitecture()))
                          .flatMap(inst -> inst.getTypes().stream())
                          .toList();
                    }));

    this.autoScalingClient.updateAutoScalingGroup(
        request ->
            request
                .autoScalingGroupName(name)
                .defaultCooldown(autoScalingGroupConfig.getDefaultCooldown())
                .healthCheckGracePeriod(autoScalingGroupConfig.getHealthcheckGracePeriod())
                .terminationPolicies(autoScalingGroupConfig.getTerminationPolicies())
                .capacityRebalance(autoScalingGroupConfig.getCapacityRebalance())
                .defaultInstanceWarmup(autoScalingGroupConfig.getDefaultInstanceWarmup())
                .mixedInstancesPolicy(
                    mixedInstancePolicyBuilder ->
                        buildMixedInstancesPolicy(
                                mixedInstancePolicyBuilder,
                                launchTemplateToInstanceTypesMap,
                                autoScalingGroupConfig)
                            .build())
                .instanceMaintenancePolicy(
                    policy ->
                        buildInstanceMaintenancePolicy(policy, autoScalingGroupConfig).build()));
    if (!autoScalingGroupConfig.getSuspendProcesses().isEmpty()) {
      this.autoScalingClient.suspendProcesses(
          request ->
              request
                  .autoScalingGroupName(name)
                  .scalingProcesses(autoScalingGroupConfig.getSuspendProcesses()));
    }
  }

  private MixedInstancesPolicy.Builder buildMixedInstancesPolicy(
      MixedInstancesPolicy.Builder mixedInstancePolicyBuilder,
      Map<String, List<String>> launchTemplateToInstanceTypesMap,
      AutoScalingGroupConfig autoScalingGroupConfig) {
    return mixedInstancePolicyBuilder
        .launchTemplate(
            launchTemplateBuilder ->
                launchTemplateBuilder
                    .launchTemplateSpecification(
                        launchTemplateSpecificationBuilder ->
                            launchTemplateSpecificationBuilder
                                .launchTemplateId(
                                    launchTemplateToInstanceTypesMap.keySet().iterator().next())
                                .version(Constants.LATEST))
                    .overrides(
                        launchTemplateToInstanceTypesMap.entrySet().stream()
                            .flatMap(
                                entry ->
                                    entry.getValue().stream()
                                        .map(
                                            instanceType ->
                                                LaunchTemplateOverrides.builder()
                                                    .instanceType(instanceType)
                                                    .launchTemplateSpecification(
                                                        launchTemplateSpecificationBuilder ->
                                                            launchTemplateSpecificationBuilder
                                                                .launchTemplateId(entry.getKey())
                                                                .version(Constants.LATEST))
                                                    .build()))
                            .toList()))
        .instancesDistribution(
            instanceDistributionBuilder ->
                instanceDistributionBuilder
                    .spotAllocationStrategy(autoScalingGroupConfig.getSpotAllocationStrategy())
                    .onDemandBaseCapacity(autoScalingGroupConfig.getOnDemandBaseCapacity())
                    .onDemandPercentageAboveBaseCapacity(
                        autoScalingGroupConfig.getOnDemandPercentageAboveBaseCapacity()));
  }

  private InstanceMaintenancePolicy.Builder buildInstanceMaintenancePolicy(
      InstanceMaintenancePolicy.Builder policyBuilder,
      AutoScalingGroupConfig autoScalingGroupConfig) {
    return policyBuilder
        .minHealthyPercentage(
            autoScalingGroupConfig.getInstanceMaintenancePolicy().getMinHealthyPercentage())
        .maxHealthyPercentage(
            autoScalingGroupConfig.getInstanceMaintenancePolicy().getMaxHealthyPercentage());
  }
}
