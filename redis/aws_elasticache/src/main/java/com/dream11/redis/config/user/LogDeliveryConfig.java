package com.dream11.redis.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogDeliveryConfig {
  @NotNull private String logType;

  @NotNull private String destinationType;

  @NotNull @Valid private Map<String, Object> destinationDetails;

  private Boolean enabled;
}
