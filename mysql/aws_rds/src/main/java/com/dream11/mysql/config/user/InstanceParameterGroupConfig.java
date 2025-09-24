package com.dream11.mysql.config.user;

import lombok.Data;

@Data
public class InstanceParameterGroupConfig {
  private Integer interactiveTimeout;
  private Integer waitTimeout;
  private Integer lockWaitTimeout;
  private Integer longQueryTime;
  private Integer maxAllowedPacket;
  private Integer slowQueryLog;
  private Integer tmpTableSize;
  private Integer maxHeapTableSize;
}
