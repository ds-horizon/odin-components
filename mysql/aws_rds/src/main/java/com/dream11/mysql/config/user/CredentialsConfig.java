package com.dream11.mysql.config.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CredentialsConfig {
  @JsonProperty("masterUsername")
  @NotNull
  @Size(min = 1, max = 16)
  private String masterUsername;

  @JsonProperty("masterUserPassword")
  @Size(min = 8, max = 128)
  private String masterUserPassword;

  @JsonProperty("manageMasterUserPassword")
  private Boolean manageMasterUserPassword = false;

  @JsonProperty("masterUserSecretKmsKeyId")
  @Pattern(regexp = "^arn:(aws|aws-cn|aws-us-gov):kms:[a-z0-9-]+:\\d{12}:key\\/[0-9a-fA-F-]{36}$")
  private String masterUserSecretKmsKeyId;
}
