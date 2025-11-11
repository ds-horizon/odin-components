package com.dream11.application.config.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageConfig {
  @NotBlank String repository;
  @NotBlank String tag;
}
