package com.dream11.redis.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LogType {
    @JsonProperty("slow-log")
    SLOW_LOG("slow-log"),

    @JsonProperty("engine-log")
    ENGINE_LOG("engine-log");

    private final String value;
}
