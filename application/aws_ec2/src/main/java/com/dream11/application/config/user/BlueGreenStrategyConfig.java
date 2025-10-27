package com.dream11.application.config.user;

import com.dream11.application.constant.ErrorMetric;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlueGreenStrategyConfig implements StrategyConfig {
  @NotNull Boolean autoRouting = Boolean.TRUE;

  @JsonProperty("passiveDownscale")
  @Valid
  @NotNull
  PassiveDownscaleConfig passiveDownscale = new PassiveDownscaleConfig();

  @JsonProperty("canary")
  @Valid
  @NotNull
  CanaryConfig canaryConfig = new CanaryConfig();

  @Data
  public static class CanaryConfig {
    @NotNull Boolean enabled = Boolean.TRUE;
    @Valid @NotNull CanaryErrorThreshold errorThreshold = new CanaryErrorThreshold();

    @Valid @NotNull Step steps = new Step();
  }

  @Data
  public static class CanaryErrorThreshold {
    @NotNull Integer value = 0;
    @NotNull ErrorMetric metric = ErrorMetric.ABSOLUTE;
  }

  @Data
  public static class Step {
    @NotNull Integer weight = 20;
    @NotNull Integer count = 1;

    @JsonProperty("duration")
    @NotNull
    Integer duration = 180;

    // method name must start with "is" to get this method invoked during validation flow
    @AssertTrue(message = "Step count * weight must be < 100")
    boolean isStepCountAndWeightValid() {
      return this.count * this.weight < 100;
    }
  }

  @AssertTrue(message = "Passive downscale must be disabled if auto routing is disabled")
  boolean isPassiveDownscaleAllowed() {
    return this.getAutoRouting() || !this.getPassiveDownscale().getEnabled();
  }
}
