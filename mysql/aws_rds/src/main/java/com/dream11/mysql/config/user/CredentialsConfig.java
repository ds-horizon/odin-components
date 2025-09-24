package com.dream11.mysql.config.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CredentialsConfig {
  @NotNull
  @Size(min = 1, max = 16)
  private String masterUsername;

  @Size(min = 8, max = 128)
  private String masterUserPassword;

  private Boolean manageMasterUserPassword = false;

  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):kms:[a-z0-9-]+:\\d{12}:key\\/[0-9a-fA-F-]{36}$")
  private String masterUserSecretKmsKeyId;
}
