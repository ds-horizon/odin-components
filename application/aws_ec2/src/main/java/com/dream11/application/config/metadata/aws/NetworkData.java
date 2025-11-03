package com.dream11.application.config.metadata.aws;

import com.dream11.application.config.Config;
import com.dream11.application.constant.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class NetworkData implements Config {

  @Valid @NotNull Subnet lbSubnets;
  @Valid @NotNull SecurityGroup lbSecurityGroups;
  @Valid @NotNull Subnet ec2Subnets;
  @Valid @NotNull SecurityGroup ec2SecurityGroups;
  @NotBlank String vpcId;

  @Data
  public static class SecurityGroup {
    @NotEmpty List<@NotBlank String> internal = new ArrayList<>();
    @NotNull List<String> external = new ArrayList<>();
  }

  @Data
  public static class Subnet {
    @JsonProperty(Constants.PRIVATE)
    @NotEmpty
    List<@NotBlank String> privateSubnets = new ArrayList<>();

    @JsonProperty(Constants.PUBLIC)
    @NotNull
    List<String> publicSubnets = new ArrayList<>();
  }
}
