package com.dream11.mysql.config.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WriterConfig {
  @NotNull
  @Pattern(
      regexp =
          "^(db\\.(?:[trm][0-9][a-z]?\\.(?:micro|small|medium|large|xlarge|\\d+x?large))|db\\.serverless)$")
  private String instanceType;

  @Min(0)
  @Max(15)
  private Integer promotionTier;
}
