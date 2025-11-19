package com.dream11.application.config.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Account {
  private String name;
  private String provider;
  private String category;
  private JsonNode data;
  private List<Service> services;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Service {
    @NotBlank String name;
    @NotBlank String category;
    @NotNull Map<String, Object> data;
  }
}
