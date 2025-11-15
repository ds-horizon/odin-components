package com.dream11.redis.config.user;

import com.dream11.redis.config.Config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateNodeTypeConfig implements Config {

    @NotNull
    @Pattern(regexp = "^cache\\.[a-z0-9]+\\.(micro|small|medium|large|xlarge|[0-9]+xlarge)$")
    private String cacheNodeType = "cache.t4g.micro";

}
