package com.dream11.application.config.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateStackConfig {

  @NotNull
  @Max(10)
  Integer stacks;

  @NotNull Boolean autoRouting = false;
}
