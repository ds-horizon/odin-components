package com.dream11.application.constant;

import java.util.Arrays;
import java.util.Optional;

public enum CanaryPhase {
  INITIALIZED("Initialized"),
  SUCCEEDED("Succeeded"),
  FAILED("Failed"),
  PROGRESSING("Progressing");

  private final String value;

  CanaryPhase(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }

  public static Optional<CanaryPhase> fromString(String phase) {
    if (phase == null) {
      return Optional.empty();
    }
    return Arrays.stream(CanaryPhase.values())
        .filter(canaryPhase -> canaryPhase.getValue().equals(phase))
        .findFirst();
  }
}
