package com.dream11.application.config.metadata;

import java.util.List;
import lombok.Data;

@Data
public class CloudProviderDetails {
  private Account account;
  private List<Account> linkedAccounts;
}
