package com.dream11.application.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ProvisionedCapacityResponse {

  @JsonProperty("ProvisionedCapacity")
  ProvisionedCapacity provisionedCapacity;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProvisionedCapacity {
    @JsonProperty("MinimumLBCapacityUnits")
    Integer lcu;

    @JsonProperty("Status")
    String status;
  }
}
