package com.dream11.application.config.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TolerationConfig {
  @NotBlank String key;
  @NotBlank String operator = "Equal";
  String value;
  @NotBlank String effect = "NoSchedule";
}
