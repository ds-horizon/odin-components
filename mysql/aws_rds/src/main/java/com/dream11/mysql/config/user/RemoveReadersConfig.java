package com.dream11.mysql.config.user;

import com.dream11.mysql.config.Config;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class RemoveReadersConfig implements Config {
  @JsonProperty("readersToRemove")
  @Valid
  @NotEmpty
  List<ReaderConfig> readers;
}
