package com.dream11.application.config.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HooksConfig {
  BaseHook imageSetup;
  BaseHook start;

  @Data
  public static class BaseHook {
    @NotBlank String script;
    @NotNull Boolean enabled;
  }
}
