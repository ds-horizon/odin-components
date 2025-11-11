package com.dream11.application.config.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HealthCheckConfig {

  @NotNull Integer healthyThreshold = 5;
  @NotNull Integer unhealthyThreshold = 2;
  @NotNull Integer timeout = 5;
  @NotNull Integer interval = 10;
  @NotEmpty String path = "/healthcheck";

  @AssertTrue(message = "healthcheck path must start with /")
  boolean isPathValid() {
    return this.path.startsWith("/");
  }
}
