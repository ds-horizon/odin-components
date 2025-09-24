package com.dream11.mysql.config.user;

import lombok.Data;

@Data
public class DeletionConfig {
  private Boolean skipFinalSnapshot = true;
  private String finalSnapshotIdentifier;
}
