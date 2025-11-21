package com.dream11.mysql.config.user;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CredentialsConfig {
  private Boolean manageMasterUserPassword;

  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):kms:[a-z0-9-]+:\\d{12}:key\\/[0-9a-fA-F-]{36}$")
  private String masterUserSecretKmsKeyId;
}
