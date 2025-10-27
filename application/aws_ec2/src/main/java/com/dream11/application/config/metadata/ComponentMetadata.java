package com.dream11.application.config.metadata;

import com.dream11.application.config.Config;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ComponentMetadata implements Config {

  @Valid @NotNull CloudProviderConfig cloudProviderDetails;

  @JsonProperty("name")
  @NotBlank
  String componentName;

  @NotBlank String envName;
}
