package com.dream11.mysql.config.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WriterConfig extends InstanceConfig {
  // Writer-specific fields (none currently - all fields are inherited from InstanceConfig)
}
