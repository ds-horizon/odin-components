package com.dream11.application.config;

import com.dream11.application.util.ApplicationUtil;

public interface Config {
  default void validate() {
    ApplicationUtil.validate(this);
  }
}
