package com.dream11.application.config.user.probe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class HttpGetConfig implements ProbeConfig {
  @NotBlank String path;
  @Positive @NotNull Integer port;
  @NotNull Map<String, String> headers = new HashMap<>();
  @NotBlank String scheme = "HTTP";
}
