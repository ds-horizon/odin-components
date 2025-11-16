package com.dream11.redis.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DestinationType {
    @JsonProperty("cloudwatch-logs")
    CLOUDWATCH_LOGS("cloudwatch-logs"),

    @JsonProperty("kinesis-firehose")
    KINESIS_FIREHOSE("kinesis-firehose");

    private final String value;
}
