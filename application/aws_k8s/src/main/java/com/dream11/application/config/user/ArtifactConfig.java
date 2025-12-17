package com.dream11.application.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArtifactConfig {
  @NotBlank String name;
  @NotBlank String version;
  @NotNull @Valid HooksConfig hooks;
}
