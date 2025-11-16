package com.dream11.redis.config;

import com.dream11.redis.util.ApplicationUtil;

public interface Config {
  default void validate() {
    ApplicationUtil.validate(this);
  }
}
