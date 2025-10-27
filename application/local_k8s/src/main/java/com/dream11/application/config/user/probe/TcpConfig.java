package com.dream11.application.config.user.probe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TcpConfig implements ProbeConfig {
  @Positive @NotNull Integer port;
}
