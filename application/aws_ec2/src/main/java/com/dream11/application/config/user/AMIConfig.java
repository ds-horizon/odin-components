package com.dream11.application.config.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class AMIConfig {
  @NotNull Map<String, String> filters;
  @NotBlank String sshUser;
  @NotBlank String buildInstanceType;
}
