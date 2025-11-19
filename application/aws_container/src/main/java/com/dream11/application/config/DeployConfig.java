package com.dream11.application.config;

import com.dream11.application.Application;
import com.dream11.application.config.user.ArtifactConfig;
import com.dream11.application.config.user.DiscoveryConfig;
import com.dream11.application.config.user.Probes;
import com.dream11.application.config.user.ResourcesConfig;
import com.dream11.application.config.user.TolerationConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.PortConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class DeployConfig implements Config {

  // Definition file
  @JsonProperty("artifact")
  @Valid
  @NotNull
  ArtifactConfig artifactConfig;

  @JsonProperty("discovery")
  @Valid
  @NotNull
  DiscoveryConfig discoveryConfig;

  // Provisioning file
  @NotNull @Valid ImageConfig baseImage;

  @NotNull Map<String, String> extraEnvVars = new HashMap<>();

  @NotNull
  @Valid
  @Size(min = 1)
  List<PortConfig> ports;

  @NotNull
  @Min(0)
  Integer replicas;

  @NotNull @Valid ResourcesConfig resources = new ResourcesConfig();

  @NotNull Map<String, String> tags = new HashMap<>();
  @NotNull Map<String, String> nodeSelector = new HashMap<>();
  @Valid @NotNull List<TolerationConfig> tolerations = new ArrayList<>();
  @NotNull Map<String, String> clusterLabels = new HashMap<>();

  @NotNull @Valid Probes probes;

  @AssertTrue(message = "Port names must be unique")
  boolean isPortsValid() {
    return this.ports.stream().map(PortConfig::getName).distinct().count() == this.ports.size();
  }

  @SneakyThrows
  public DeployConfig mergeWith(String overrides) {
    ObjectMapper objectMapper = Application.getObjectMapper();
    JsonNode node = objectMapper.readValue(objectMapper.writeValueAsString(this), JsonNode.class);
    return objectMapper.readValue(
        objectMapper.readerForUpdating(node).readValue(overrides).toString(), DeployConfig.class);
  }
}
