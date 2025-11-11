package com.dream11.application.config.metadata.local;

import com.dream11.application.config.Config;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DockerRegistryData implements Config {
  @NotBlank String registry;
  @NotBlank String server;
  @NotNull String username = "";
  @NotNull String password = "";
  @NotNull boolean allowPush = false;
}
