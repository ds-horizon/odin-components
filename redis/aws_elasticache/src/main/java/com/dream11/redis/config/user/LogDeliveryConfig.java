package com.dream11.redis.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LogDeliveryConfig {
    @NotNull
    private LogType logType;

    @NotNull
    private DestinationType destinationType;

    @NotNull
    @Valid
    private DestinationDetails destinationDetails;

    private Boolean enabled;

    private LogFormat logFormat;
}
