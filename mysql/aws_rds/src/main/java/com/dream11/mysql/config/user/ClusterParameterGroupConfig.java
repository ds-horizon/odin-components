package com.dream11.mysql.config.user;

import com.dream11.mysql.annotation.ParameterName;
import lombok.Data;

@Data
public class ClusterParameterGroupConfig {
  @ParameterName("binlog_format")
  private String binlogFormat;
  
  @ParameterName("innodb_print_all_deadlocks")
  private String innodbPrintAllDeadlocks;
  
  @ParameterName("aws_default_lambda_role")
  private String awsDefaultLambdaRole;
  
  @ParameterName("aws_default_s3_role")
  private String awsDefaultS3Role;
  
  @ParameterName("aws_default_logs_role")
  private String awsDefaultLogsRole;
}
