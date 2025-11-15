package com.dream11.redis.config.user;

import com.dream11.redis.config.Config;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateNodeGroupCountConfig implements Config {

    @NotNull
    private Integer numNodeGroups;

}
