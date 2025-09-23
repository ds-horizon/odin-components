package com.dream11.mysql.config;

import com.dream11.mysql.util.ApplicationUtil;

public interface Config {
  default void validate() {
    ApplicationUtil.validate(this);
  }
}
