package com.dream11.application.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudWatchMetric {
  @Builder.Default String statistic = "Sum";
  String namespace;
  String metricName;
  String resourceName;
  String resourceValue;
}
