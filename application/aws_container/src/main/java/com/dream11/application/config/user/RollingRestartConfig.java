package com.dream11.application.config.user;

import com.dream11.application.config.Config;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RollingRestartConfig implements Config {
  @NotNull
  @Min(1)
  @Max(100)
  Integer batchSizePercentage = 10;
}
