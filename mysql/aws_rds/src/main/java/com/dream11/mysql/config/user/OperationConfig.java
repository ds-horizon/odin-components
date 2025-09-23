package com.dream11.mysql.config.user;

import com.dream11.mysql.Application;
import com.dream11.mysql.config.Config;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class OperationConfig implements Config {
  @SneakyThrows
  public OperationConfig deepCopy() {
    return Application.getObjectMapper()
        .readValue(Application.getObjectMapper().writeValueAsString(this), OperationConfig.class);
  }
}
