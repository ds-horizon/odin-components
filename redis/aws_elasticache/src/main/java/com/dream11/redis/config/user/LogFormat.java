package com.dream11.redis.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LogFormat {
    @JsonProperty("json")
    JSON("json"),

    @JsonProperty("text")
    TEXT("text");

    private final String value;
}
