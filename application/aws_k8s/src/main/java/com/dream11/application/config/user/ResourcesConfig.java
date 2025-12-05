package com.dream11.application.config.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourcesConfig {

  @NotNull Resources requests;
  @NotNull Resources limits;

  @Data
  public static final class Resources {
    @NotBlank String cpu;
    @NotBlank String memory;
  }
}
