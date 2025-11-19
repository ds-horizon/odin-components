package com.dream11.application.config.aws;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class EKSData {
  @NotBlank List<String> pullSecrets;
  @NotNull Map<String, String> serviceAnnotations = new HashMap<>();
  @NotNull Map<String, String> serviceAccountAnnotations = new HashMap<>();
  @NotNull Map<String, String> environmentVariables = new HashMap<>();
}
