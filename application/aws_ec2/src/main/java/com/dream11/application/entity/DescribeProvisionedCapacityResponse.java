package com.dream11.application.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DescribeProvisionedCapacityResponse {

  @JsonProperty("DescribeProvisionedCapacityResult")
  ProvisionedCapacityResponse provisionedCapacityResponse;
}
