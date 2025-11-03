package com.dream11.application.service;

import com.dream11.application.Application;
import com.dream11.application.aws.AutoscalingGroupClient;
import com.dream11.application.config.metadata.aws.AwsAccountData;
import com.dream11.application.config.metadata.aws.EC2Data;
import com.dream11.application.config.metadata.aws.NetworkData;
import com.dream11.application.config.user.AutoScalingGroupConfig;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DeploymentStrategy;
import com.dream11.application.state.AutoscalingGroupState;
import com.dream11.application.state.State;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class AutoscalingGroupService {

  @NonNull final DeployConfig deployConfig;

  @NonNull final AutoscalingGroupClient autoscalingGroupClient;

  @NonNull final EC2Data ec2Data;
  @NonNull final NetworkData networkData;

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

  public AutoScalingGroup createAsg(
      String name,
      List<String> targetGroupArns,
      List<String> loadBalancerNames,
      Map<String, String> launchTemplateIdArchitectureMap,
      AutoScalingGroupConfig autoScalingGroupConfig,
      Map<String, String> extraTags) {
    AutoScalingGroup autoScalingGroup =
        this.autoscalingGroupClient.create(
            name,
            targetGroupArns,
            loadBalancerNames,
            launchTemplateIdArchitectureMap,
            this.networkData.getEc2Subnets().getPrivateSubnets(),
            autoScalingGroupConfig,
            ApplicationUtil.merge(
                List.of(
                    this.tags,
                    extraTags,
                    Map.of(
                        Constants.NAME_TAG,
                        name,
                        Constants.ARTIFACT_NAME_TAG,
                        this.deployConfig.getArtifactConfig().getName(),
                        Constants.ARTIFACT_VERSION_TAG,
                        this.deployConfig.getArtifactConfig().getVersion(),
                        Constants.DEPLOYMENT_STRATEGY_TAG,
                        DeploymentStrategy.BLUE_GREEN.name()))));
    log.info("ASG:[{}] created successfully", name);
    Application.getState()
        .addAsgState(autoScalingGroup, launchTemplateIdArchitectureMap.keySet().stream().toList());
    return autoScalingGroup;
  }

  public void detachTgFromAsg(List<String> targetGroupARNs) {
    targetGroupARNs.forEach(
        targetGroupARN -> {
          Optional<AutoscalingGroupState> autoscalingGroupState =
              Application.getState().getAsgStateFromTgArn(targetGroupARN);
          if (autoscalingGroupState.isPresent()) {
            this.autoscalingGroupClient.detachTargetGroups(
                autoscalingGroupState.get().getName(),
                autoscalingGroupState.get().getTargetGroupArns());
            log.info(
                "Detached target groups:{} from ASG:[{}]",
                autoscalingGroupState.get().getTargetGroupArns(),
                autoscalingGroupState.get().getName());
            autoscalingGroupState.get().setTargetGroupArns(new ArrayList<>());
          }
        });
  }

  public void detachLbFromAsg(List<String> loadBalancerNames) {
    loadBalancerNames.forEach(
        loadBalancerName -> {
          Optional<AutoscalingGroupState> autoscalingGroupState =
              Application.getState().getAsgStateFromLoadBalancerName(loadBalancerName);
          if (autoscalingGroupState.isPresent()) {
            this.autoscalingGroupClient.detachLoadBalancers(
                autoscalingGroupState.get().getName(),
                autoscalingGroupState.get().getLoadBalancerNames());
            log.info(
                "Detached load balancers:{} from ASG:[{}]",
                autoscalingGroupState.get().getLoadBalancerNames(),
                autoscalingGroupState.get().getName());
            autoscalingGroupState.get().setLoadBalancerNames(new ArrayList<>());
          }
        });
  }

  public void deleteAsgs() {
    log.info("Deleting ASGs");
    State state = Application.getState();
    List.copyOf(state.getAsg())
        .forEach(autoscalingGroupState -> this.deleteAsg(autoscalingGroupState.getName()));
  }

  public void deleteAsg(String name) {
    State state = Application.getState();
    log.debug("Deleting ASG:[{}]", name);
    this.autoscalingGroupClient.delete(name);
    log.info("Deleted ASG:[{}]", name);
    state.removeAsgState(name);
  }

  public void scale(String name, Integer desiredCapacity) {
    this.autoscalingGroupClient.setDesiredCapacity(name, desiredCapacity);
    log.info("Scaled ASG:[{}] to {} instances", name, desiredCapacity);
  }

  public void scale(String name, Integer desiredCapacity, Integer maxSize) {
    this.autoscalingGroupClient.setDesiredCapacity(name, desiredCapacity, maxSize);
    log.info(
        "Scaled ASG:[{}] to [{}] instances and max size: [{}]", name, desiredCapacity, maxSize);
  }

  public long getInServiceInstances(String name) {
    long healthyInstances =
        this.autoscalingGroupClient.describe(name).instances().stream()
            .filter(instance -> instance.lifecycleState().equals(LifecycleState.IN_SERVICE))
            .count();
    log.debug("Number of healthy instances in ASG:[{}] is {}", name, healthyInstances);
    return healthyInstances;
  }

  public AutoScalingGroup describe(String name) {
    return this.autoscalingGroupClient.describe(name);
  }

  public Set<String> getAsgsFromTgArns(List<String> tgArns) {
    Set<String> asgs = new HashSet<>();
    tgArns.forEach(
        tgArn ->
            Application.getState()
                .getAsgStateFromTgArn(tgArn)
                .ifPresent(asgState -> asgs.add(asgState.getName())));
    return asgs;
  }

  public Set<String> getAsgsFromLbNames(List<String> lbNames) {
    Set<String> asgs = new HashSet<>();
    lbNames.forEach(
        lbName ->
            Application.getState()
                .getAsgStateFromLoadBalancerName(lbName)
                .ifPresent(asgState -> asgs.add(asgState.getName())));
    return asgs;
  }

  public void updateTags(String name, Map<String, String> tags) {
    log.info("Updating tags:[{}] for asg:[{}]", tags, name);
    this.autoscalingGroupClient.updateTag(name, tags);
  }

  public void updateAsg(List<String> asgs) {
    asgs.forEach(
        asg ->
            this.autoscalingGroupClient.updateAsg(
                asg, this.deployConfig.getAutoScalingGroupConfig()));
  }
}
