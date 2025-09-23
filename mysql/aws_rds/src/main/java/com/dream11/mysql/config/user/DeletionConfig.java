package com.dream11.mysql.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeletionConfig {
  @JsonProperty("skipFinalSnapshot")
  private Boolean skipFinalSnapshot = true;

  @JsonProperty("finalSnapshotIdentifier")
  private String finalSnapshotIdentifier;
}
