package com.dream11.application.config.user;

import com.dream11.application.config.user.probe.GrpcConfig;
import com.dream11.application.config.user.probe.HttpGetConfig;
import com.dream11.application.config.user.probe.TcpConfig;
import com.dream11.application.constant.ProbeType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Probes {

  @NotNull @Valid Probe liveness;
  @NotNull @Valid Probe readiness;
  @NotNull @Valid Probe startup;

  @Data
  public static class Probe {
    @NotNull ProbeType type;
    @NotNull boolean enabled = true;

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "type")
    @JsonSubTypes({
      @JsonSubTypes.Type(value = HttpGetConfig.class, name = "HTTP_GET"),
      @JsonSubTypes.Type(value = GrpcConfig.class, name = "GRPC"),
      @JsonSubTypes.Type(value = TcpConfig.class, name = "TCP")
    })
    @NotNull
    @Valid
    com.dream11.application.config.user.probe.ProbeConfig config;

    @NotNull Integer initialDelaySeconds;
    @NotNull Integer intervalSeconds;
    @NotNull Integer timeoutSeconds;
    @NotNull Integer successThreshold;
    @NotNull Integer failureThreshold;
  }
}
