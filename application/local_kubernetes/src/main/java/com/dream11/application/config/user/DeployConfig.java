package com.dream11.application.config.user;

import com.dream11.application.Application;
import com.dream11.application.config.Config;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

  // Provisioning file
  @NotNull @Valid ImageConfig baseImage;

  @NotNull Map<String, String> extraEnvVars = new HashMap<>();

  @NotNull
  @Valid
  @Size(min = 1)
  List<PortConfig> ports;

  @NotNull @Valid ResourcesConfig resources = new ResourcesConfig();

  @NotNull
  @Min(0)
  Integer replicas;

  @NotNull @Valid Probes probes;

  @NotNull @Valid LocalArtifactConfig localArtifact = new LocalArtifactConfig();

  @SneakyThrows
  public DeployConfig mergeWith(String overrides) {
    ObjectMapper objectMapper = Application.getObjectMapper();
    JsonNode node = objectMapper.readValue(objectMapper.writeValueAsString(this), JsonNode.class);
    return objectMapper.readValue(
        objectMapper.readerForUpdating(node).readValue(overrides).toString(), DeployConfig.class);
  }
}
