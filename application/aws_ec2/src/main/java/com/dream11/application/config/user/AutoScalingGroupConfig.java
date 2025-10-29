package com.dream11.application.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AutoScalingGroupConfig {

  @NotNull Integer healthcheckGracePeriod = 120;
  @NotNull Integer onDemandBaseCapacity = 0;
  @NotNull Integer onDemandPercentageAboveBaseCapacity = 0;
  @NotNull Integer desiredInstances = 1;
  @NotNull Integer maxInstances = 1000;
  @NotNull Integer initialCapacity = 1;

  @NotNull
  @Size(min = 1)
  List<Instance> instances;

  @Data
  public static class Instance {
    String architecture;
    List<String> types;
  }

  @NotNull String spotAllocationStrategy = "price-capacity-optimized";
  @NotNull List<String> terminationPolicies = new ArrayList<>();
  @NotNull List<String> suspendProcesses = new ArrayList<>();
  String snsTopicArn;
  @NotNull Boolean capacityRebalance = Boolean.TRUE;

  @NotNull @Valid
  InstanceMaintenancePolicy instanceMaintenancePolicy = new InstanceMaintenancePolicy();

  @Data
  public static class InstanceMaintenancePolicy {
    @NotNull Integer minHealthyPercentage = 100;
    @NotNull Integer maxHealthyPercentage = 110;
  }

  @NotNull Integer defaultCooldown = 300;
  @NotNull Integer defaultInstanceWarmup = 0;
  @NotNull String imdsv2 = "required";
}
