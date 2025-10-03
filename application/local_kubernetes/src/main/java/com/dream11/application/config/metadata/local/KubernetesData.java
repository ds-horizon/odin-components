package com.dream11.application.config.metadata.local;

import com.dream11.application.config.Config;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class KubernetesData implements Config {
  @NotBlank List<String> pullSecrets;
  @NotNull Map<String, String> environmentVariables = new HashMap<>();
}
