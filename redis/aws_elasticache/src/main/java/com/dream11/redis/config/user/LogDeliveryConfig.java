package com.dream11.redis.config.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LogDeliveryConfig {
    @NotNull
    private String logType; // "slow-log" or "engine-log"

    @NotNull
    private String destinationType; // "cloudwatch-logs" or "kinesis-firehose"

    @NotNull
    private DestinationDetails destinationDetails;

    private Boolean enabled;

    private String logFormat; // "json" or "text"
}
