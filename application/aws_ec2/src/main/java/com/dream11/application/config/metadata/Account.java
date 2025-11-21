package com.dream11.application.config.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Account {
  @NotBlank String name;
  @NotBlank String provider;
  @NotBlank String category;
  @NotNull List<@Valid Service> services = new ArrayList<>();
  @NotNull Map<String, Object> data = new HashMap<>();

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Service {
    @NotBlank String name;
    @NotBlank String category;
    @NotNull Map<String, Object> data = new HashMap<>();
  }
}
