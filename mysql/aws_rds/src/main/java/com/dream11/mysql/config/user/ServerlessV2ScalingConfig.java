package com.dream11.mysql.config.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServerlessV2ScalingConfig {
  @NotNull
  @DecimalMin("0.5")
  private Double minCapacity;

  @NotNull
  @DecimalMin("0.5")
  private Double maxCapacity;
}
