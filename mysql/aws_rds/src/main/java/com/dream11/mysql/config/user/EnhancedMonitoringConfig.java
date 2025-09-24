package com.dream11.mysql.config.user;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnhancedMonitoringConfig {
  private Boolean enabled = false;
  private Integer interval = 60;

  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):iam::\\d{12}:role\\/.+")
  private String monitoringRoleArn;
}
