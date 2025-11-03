package com.dream11.application.config.user;

import com.dream11.application.constant.Constants;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PassiveDownscaleConfig {
  @NotNull Boolean enabled = Boolean.TRUE;
  @NotNull Long delay = Constants.ROUTE_53_TTL;
}
