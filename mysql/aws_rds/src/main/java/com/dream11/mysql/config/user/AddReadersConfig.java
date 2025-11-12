package com.dream11.mysql.config.user;

import com.dream11.mysql.config.Config;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AddReadersConfig implements Config {
  @JsonProperty("readersToAdd")
  @Valid
  @NotEmpty
  List<ReaderConfig> readers = new ArrayList<>();
}
