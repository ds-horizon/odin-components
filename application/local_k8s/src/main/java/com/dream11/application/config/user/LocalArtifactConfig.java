package com.dream11.application.config.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;

@Data
public class LocalArtifactConfig {
  @NotNull boolean enabled = false;
  @NonNull String path = "";
}
