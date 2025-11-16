package com.dream11.redis.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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

    @AssertTrue(message = "destinationDetails.cloudWatchLogsDetails is required when destinationType is cloudwatch-logs")
    boolean isCloudWatchLogsDetailsValid() {
        if (destinationType == DestinationType.CLOUDWATCH_LOGS) {
            return destinationDetails != null && destinationDetails.getCloudWatchLogsDetails() != null;
        }
        return true;
    }

    @AssertTrue(message = "destinationDetails.kinesisFirehoseDetails is required when destinationType is kinesis-firehose")
    boolean isKinesisFirehoseDetailsValid() {
        if (destinationType == DestinationType.KINESIS_FIREHOSE) {
            return destinationDetails != null && destinationDetails.getKinesisFirehoseDetails() != null;
        }
        return true;
    }
}
