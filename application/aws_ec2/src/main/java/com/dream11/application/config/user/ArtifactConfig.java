package com.dream11.application.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArtifactConfig {
  @NotBlank String name;
  @NotBlank String version;
  @NotNull @Valid HooksConfig hooks;

  @AssertTrue(message = "length(artifact name) + length(artifact version) <= 80 characters")
  boolean isNameAndVersionLengthWithinLimits() {
    return this.name.length() + this.version.length() <= 80;
  }
}
