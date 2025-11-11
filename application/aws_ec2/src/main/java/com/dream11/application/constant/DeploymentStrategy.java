package com.dream11.application.constant;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DeploymentStrategy {
  @JsonProperty("blue-green")
  BLUE_GREEN
}
