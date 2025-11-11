package com.dream11.application.config.user;

import com.dream11.application.constant.LoadBalancerType;
import com.dream11.application.constant.Protocol;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class LoadBalancerConfig {
  @NotNull LoadBalancerType type = LoadBalancerType.ALB;

  @JsonProperty("lcus")
  @Valid
  @NotNull
  LcuConfig lcuConfig = new LcuConfig();

  @NotNull @Valid List<Listener> listeners = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Listener {
    @NotNull Integer port;
    @NotNull Protocol protocol;
    @NotNull Integer targetPort;
    @NotNull Protocol targetProtocol;

    @JsonProperty("healthchecks")
    @Valid
    @NotNull
    HealthCheckConfig healthChecks = new HealthCheckConfig();

    @AssertTrue(
        message =
            "either both protocol and targetProtocol should be TCP, or if the targetProtocol is GRPC, then the protocol should be HTTPS")
    boolean isValidProtocolCombination() {
      return !(((this.protocol == Protocol.TCP || this.targetProtocol == Protocol.TCP)
              && (this.protocol != this.targetProtocol))
          || this.targetProtocol == Protocol.GRPC && this.protocol != Protocol.HTTPS);
    }
  }
}
