package com.dream11.application.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoadBalancerType {
  ALB("application"),
  CLB("classic"),
  NLB("network");

  final String value;
}
