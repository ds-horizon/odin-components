package com.dream11.application.operation;

import com.dream11.application.service.RollingRestartService;
import com.dream11.application.service.StateCorrectionService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RollingRestart implements Operation {

  @NonNull final RollingRestartService rollingRestartService;
  @NonNull final StateCorrectionService stateCorrectionService;

  @Override
  public boolean execute() {
    this.stateCorrectionService.correctState();
    this.rollingRestartService.rollingRestart();
    return false;
  }
}
