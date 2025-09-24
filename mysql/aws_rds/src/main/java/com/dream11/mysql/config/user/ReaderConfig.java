package com.dream11.mysql.config.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReaderConfig extends InstanceConfig {
  @NotNull
  @Min(1)
  private Integer instanceCount;
}
