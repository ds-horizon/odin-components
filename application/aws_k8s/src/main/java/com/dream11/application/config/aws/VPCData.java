package com.dream11.application.config.aws;

import lombok.Data;

@Data
public class VPCData {
  private LBSecurityGroups lbSecurityGroups;

  @Data
  public class LBSecurityGroups {
    private String[] external;
    private String[] internal;
  }
}
