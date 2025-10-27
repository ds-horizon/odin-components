package com.dream11.application.config.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class HooksConfig {

  @NotNull @Valid PreDeployHook preDeploy;
  @NotNull @Valid PostDeployHook postDeploy;
  @NotNull @Valid BaseHook start;
  @NotNull @Valid BaseHook stop;
  @NotNull @Valid BaseHook imageSetup;

  @Data
  public static class BaseHook {
    @NotBlank String script;
    @NotNull Boolean enabled;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class PreDeployHook extends BaseHook {
    @NotBlank String dockerImage;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class PostDeployHook extends BaseHook {
    @NotBlank String dockerImage;
  }
}
