package com.dream11.application.config.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortConfig {
  @NotNull Integer port;
  @NotNull Integer targetPort;
}
