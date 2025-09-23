package com.dream11.mysql.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServerlessV2ScalingConfig {
  @JsonProperty("minCapacity")
  @NotNull
  @DecimalMin("0.5")
  private Double minCapacity;

  @JsonProperty("maxCapacity")
  @NotNull
  @DecimalMin("0.5")
  private Double maxCapacity;
}
