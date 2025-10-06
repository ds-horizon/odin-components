package com.dream11.mysql.config.user;

import com.dream11.mysql.annotation.ParameterName;
import lombok.Data;

@Data
public class InstanceParameterGroupConfig {
  @ParameterName("interactive_timeout")
  private Integer interactiveTimeout;
  
  @ParameterName("wait_timeout")
  private Integer waitTimeout;
  
  @ParameterName("lock_wait_timeout")
  private Integer lockWaitTimeout;
  
  @ParameterName("long_query_time")
  private Integer longQueryTime;
  
  @ParameterName("max_allowed_packet")
  private Integer maxAllowedPacket;
  
  @ParameterName("slow_query_log")
  private Integer slowQueryLog;
  
  @ParameterName("tmp_table_size")
  private Integer tmpTableSize;
  
  @ParameterName("max_heap_table_size")
  private Integer maxHeapTableSize;
}
