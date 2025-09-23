package com.dream11.mysql.config.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudProviderConfig {

  @Valid @NotNull Account account;

  @JsonProperty("linked_accounts")
  @Valid
  List<Account> linkedAccounts;
}
