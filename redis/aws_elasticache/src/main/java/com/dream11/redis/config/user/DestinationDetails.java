package com.dream11.redis.config.user;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class DestinationDetails {
    @Valid
    private CloudWatchLogsDetails cloudWatchLogsDetails;

    @Valid
    private KinesisFirehoseDetails kinesisFirehoseDetails;
}
