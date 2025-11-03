package com.dream11.application.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RevertConfig {
  @JsonProperty("passiveDownscale")
  @Valid
  @NotNull
  PassiveDownscaleConfig passiveDownscale = new PassiveDownscaleConfig();
}
