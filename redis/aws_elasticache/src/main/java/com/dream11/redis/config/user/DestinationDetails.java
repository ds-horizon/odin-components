package com.dream11.redis.config.user;

import lombok.Data;

@Data
public class DestinationDetails {
    private CloudWatchLogsDetails cloudWatchLogsDetails;

    private KinesisFirehoseDetails kinesisFirehoseDetails;
}
