package com.dream11.application.service;

import com.dream11.application.config.user.RollingRestartConfig;
import com.dream11.application.constant.Mode;
import com.dream11.application.entity.SSMCommand;
import com.dream11.application.util.ApplicationUtil;
import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RollingRestartService {

  @NonNull final RollingRestartConfig rollingRestartConfig;

  @NonNull final SystemsManagerService systemsManagerService;

  @NonNull final DeploymentService deploymentService;

  public void rollingRestart() {
    log.info("Rolling restart config:[{}]", rollingRestartConfig);
    this.performRollingRestart(this.rollingRestartConfig.getMode());
  }

  private void performRollingRestart(Mode mode) {
    List<String> asgsToRestart = this.deploymentService.getActiveAsgs();
    log.info("Autoscaling groups to restart:{}", asgsToRestart);
    List<Callable<Boolean>> rollingRestartTasks =
        asgsToRestart.stream()
            .map(
                asgName ->
                    (Callable<Boolean>)
                        () ->
                            this.systemsManagerService.executeAndWait(
                                asgName,
                                SSMCommand.builder()
                                    .commands(
                                        this.deploymentService.createApplicationRestartCommands(
                                            mode, asgName))
                                    .description("rr")
                                    .batchSizePercentage(
                                        this.rollingRestartConfig.getBatchSizePercentage())
                                    .errorTolerancePercentage(
                                        this.rollingRestartConfig.getErrorTolerancePercentage())
                                    .build()))
            .toList();
    ApplicationUtil.runOnExecutorService(rollingRestartTasks, false);
  }
}
