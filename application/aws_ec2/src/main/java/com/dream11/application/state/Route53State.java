package com.dream11.application.state;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Route53State {
  String route;
  String hostedZoneId;
  Set<String> identifiers = new HashSet<>();
}
