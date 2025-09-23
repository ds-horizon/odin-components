package com.dream11.mysql.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InstanceParameterGroupConfig {
  @JsonProperty("interactiveTimeout")
  private Integer interactiveTimeout;

  @JsonProperty("waitTimeout")
  private Integer waitTimeout;

  @JsonProperty("lockWaitTimeout")
  private Integer lockWaitTimeout;

  @JsonProperty("longQueryTime")
  private Integer longQueryTime;

  @JsonProperty("maxAllowedPacket")
  private Integer maxAllowedPacket;

  @JsonProperty("slowQueryLog")
  private Integer slowQueryLog;

  @JsonProperty("tmpTableSize")
  private Integer tmpTableSize;

  @JsonProperty("maxHeapTableSize")
  private Integer maxHeapTableSize;
}
