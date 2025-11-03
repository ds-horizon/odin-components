package com.dream11.application.state;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoscalingGroupState {
  String name;
  List<String> ltIds;
  List<String> loadBalancerNames; // For classic load balancers;
  List<String> targetGroupArns; // For application/network load balancers;
}
