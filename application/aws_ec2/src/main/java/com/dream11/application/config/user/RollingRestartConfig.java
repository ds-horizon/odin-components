package com.dream11.application.config.user;

import com.dream11.application.config.Config;
import com.dream11.application.constant.Mode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NonNull;

@Data
public class RollingRestartConfig implements Config {
  Mode mode = Mode.FORCEKILL;

  @NonNull
  @Min(1)
  @Max(100)
  Integer batchSizePercentage = 10;

  @NonNull
  @Min(0)
  @Max(100)
  Integer errorTolerancePercentage = 3;
}
