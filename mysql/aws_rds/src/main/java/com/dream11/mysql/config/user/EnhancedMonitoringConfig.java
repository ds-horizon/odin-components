package com.dream11.mysql.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnhancedMonitoringConfig {
  @JsonProperty("enabled")
  private Boolean enabled = false;

  @JsonProperty("interval")
  private Integer interval = 60;

  @JsonProperty("monitoringRoleArn")
  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):iam::\\d{12}:role\\/.+")
  private String monitoringRoleArn;
}
