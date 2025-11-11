package com.dream11.application.config.metadata.aws;

import com.dream11.application.config.Config;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class EC2Data implements Config {
  @NotBlank String ec2KeyName;
  @NotBlank String iamInstanceProfile;
  @Valid @NotNull AMI ami;
  @Valid @NotNull UserData userData;
  @NotNull Map<String, String> tags = new HashMap<>();

  @Data
  public static class AMI {
    @NotEmpty List<@NotBlank String> sharedAccountIds;
  }

  @Data
  public static class UserData {
    @NotNull Map<String, String> environmentVariables;
    @NotBlank String preStart;
    @NotBlank String postStart;
  }
}
