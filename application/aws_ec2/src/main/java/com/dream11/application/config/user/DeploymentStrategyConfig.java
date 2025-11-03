package com.dream11.application.config.user;

import com.dream11.application.constant.DeploymentStrategy;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeploymentStrategyConfig {
  @NotNull DeploymentStrategy name;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "name")
  @JsonSubTypes({@JsonSubTypes.Type(value = BlueGreenStrategyConfig.class, name = "blue-green")})
  @Valid
  @NotNull
  StrategyConfig config;
}
