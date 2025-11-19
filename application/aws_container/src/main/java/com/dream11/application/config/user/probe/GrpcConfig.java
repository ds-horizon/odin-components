package com.dream11.application.config.user.probe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GrpcConfig implements ProbeConfig {
  @NotNull String service;
  @Positive @NotNull Integer port;
}
