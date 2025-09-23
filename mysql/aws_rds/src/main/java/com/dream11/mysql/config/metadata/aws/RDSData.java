package com.dream11.mysql.config.metadata.aws;

import com.dream11.mysql.config.Config;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class RDSData implements Config {
  @Valid @NotNull List<String> subnetGroups;
  @Valid @NotNull List<String> securityGroups;
}
