package com.dream11.mysql.config.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnhancedMonitoringConfig {
  @NotNull private Boolean enabled;
  private Integer interval;

  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):iam::\\d{12}:role\\/.+")
  private String monitoringRoleArn;
}
