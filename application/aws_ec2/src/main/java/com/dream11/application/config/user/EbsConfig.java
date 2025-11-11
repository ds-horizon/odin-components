package com.dream11.application.config.user;

import com.dream11.application.config.Config;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EbsConfig implements Config {

  @NotNull @Positive Integer size = 50;
}
