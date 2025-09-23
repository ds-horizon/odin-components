package com.dream11.mysql.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClusterParameterGroupConfig {
  @JsonProperty("binlogFormat")
  private String binlogFormat;

  @JsonProperty("innodbPrintAllDeadlocks")
  private String innodbPrintAllDeadlocks;

  @JsonProperty("awsDefaultLambdaRole")
  private String awsDefaultLambdaRole;

  @JsonProperty("awsDefaultS3Role")
  private String awsDefaultS3Role;

  @JsonProperty("awsDefaultLogsRole")
  private String awsDefaultLogsRole;
}
