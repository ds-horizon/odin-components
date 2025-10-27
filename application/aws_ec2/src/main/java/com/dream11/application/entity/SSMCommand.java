package com.dream11.application.entity;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SSMCommand {
  List<String> commands;
  Integer batchSizePercentage;
  Integer errorTolerancePercentage;
  String description;
}
