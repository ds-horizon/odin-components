package com.dream11.application.operation;

import com.dream11.application.service.AMIService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class AMITemplate implements Operation {
  @NonNull final AMIService amiService;

  @Override
  public boolean execute() {
    this.amiService.createAMIIfNotExist();
    return false;
  }
}
