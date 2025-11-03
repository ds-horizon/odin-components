package com.dream11.mysql.config.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeletionConfig {
  @NotNull private Boolean skipFinalSnapshot;
  private String finalSnapshotIdentifier;
}
