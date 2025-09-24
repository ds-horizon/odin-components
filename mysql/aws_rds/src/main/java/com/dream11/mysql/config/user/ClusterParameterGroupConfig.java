package com.dream11.mysql.config.user;

import lombok.Data;

@Data
public class ClusterParameterGroupConfig {
  private String binlogFormat;
  private String innodbPrintAllDeadlocks;
  private String awsDefaultLambdaRole;
  private String awsDefaultS3Role;
  private String awsDefaultLogsRole;
}
