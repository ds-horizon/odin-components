package com.dream11.application.config.user;

import com.dream11.application.Application;
import com.dream11.application.config.Config;
import com.dream11.application.constant.Constants;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class DeployConfig implements Config {
  // Definition file
  @JsonProperty("artifact")
  @Valid
  @NotNull
  ArtifactConfig artifactConfig;

  @JsonProperty("baseImages")
  @Valid
  @NotNull
  List<AMIConfig> amiConfigs;

  @JsonProperty("discovery")
  @Valid
  @NotNull
  DiscoveryConfig discoveryConfig;

  // Provisioning file
  @NotNull Map<String, String> extraEnvVars = new HashMap<>();

  @NotNull
  @Min(1)
  Integer stacks = 1;

  @JsonProperty("asg")
  @Valid
  AutoScalingGroupConfig autoScalingGroupConfig = new AutoScalingGroupConfig();

  @JsonProperty("loadBalancer")
  @Valid
  @NotNull
  LoadBalancerConfig loadBalancerConfig = new LoadBalancerConfig();

  @JsonProperty("strategy")
  @Valid
  @NotNull
  DeploymentStrategyConfig deploymentStrategyConfig;

  @Valid
  @NotNull
  @JsonProperty("ebs")
  EbsConfig ebsConfig = new EbsConfig();

  @NotNull Map<String, String> tags = new HashMap<>();

  @Override
  public void validate() {
    Config.super.validate();
    validateArchitectureUniqueness();
  }

  private void validateArchitectureUniqueness() {
    Set<String> amiArchitectureSet = new HashSet<>();
    List<String> duplicateArchitectures =
        this.amiConfigs.stream()
            .map(amiConfig -> amiConfig.getFilters().get(Constants.ARCHITECTURE_IMAGE_FILTER))
            .filter(architecture -> !amiArchitectureSet.add(architecture))
            .toList();
    if (!duplicateArchitectures.isEmpty()) {
      throw new GenericApplicationException(
          ApplicationError.DUPLICATE_ARCHITECTURE_IN_AMI,
          String.join(", ", duplicateArchitectures));
    }
    Set<String> instanceArchitectureSet = new HashSet<>();
    duplicateArchitectures =
        this.autoScalingGroupConfig.getInstances().stream()
            .map(AutoScalingGroupConfig.Instance::getArchitecture)
            .filter(architecture -> !instanceArchitectureSet.add(architecture))
            .toList();
    if (!duplicateArchitectures.isEmpty()) {
      throw new GenericApplicationException(
          ApplicationError.DUPLICATE_ARCHITECTURE_IN_INSTANCES,
          String.join(", ", duplicateArchitectures));
    }
    if (!amiArchitectureSet.containsAll(instanceArchitectureSet)) {
      Set<String> extras = new HashSet<>(instanceArchitectureSet);
      amiArchitectureSet.removeAll(extras);
      throw new GenericApplicationException(
          ApplicationError.INVALID_INSTANCE_ARCHITECTURE, String.join(", ", extras));
    }
  }

  @SneakyThrows
  public DeployConfig deepCopy() {
    return Application.getObjectMapper()
        .readValue(Application.getObjectMapper().writeValueAsString(this), DeployConfig.class);
  }

  @SneakyThrows
  public DeployConfig mergeWith(String overrides) {
    ObjectMapper objectMapper = Application.getObjectMapper();
    JsonNode node = objectMapper.readValue(objectMapper.writeValueAsString(this), JsonNode.class);
    return objectMapper.readValue(
        objectMapper.readerForUpdating(node).readValue(overrides).toString(), DeployConfig.class);
  }
}
