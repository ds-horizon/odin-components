package com.dream11.application.config.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LcuConfig {
  @NotNull
  @Min(0)
  Integer internal = 1;

  @NotNull
  @Min(0)
  Integer external = 1;
}
