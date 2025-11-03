package com.dream11.application.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Route53Record {

  String name;
  String dns;
  Long ttl;
  Long weight;
  String identifier;

  public Route53Record setWeight(Long weight) {
    this.weight = weight;
    return this;
  }
}
