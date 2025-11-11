package com.dream11.application.operation;

import com.dream11.application.service.StatusService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Status implements Operation {
  @NonNull final StatusService statusService;

  @Override
  public boolean execute() {
    this.statusService.getStatus();
    return false;
  }
}
