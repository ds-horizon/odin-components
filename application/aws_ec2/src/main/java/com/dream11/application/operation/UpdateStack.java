package com.dream11.application.operation;

import com.dream11.application.service.UpdateStackService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class UpdateStack implements Operation {

  @NonNull final UpdateStackService updateStackService;

  @Override
  public boolean execute() {
    updateStackService.updateStack();
    return true;
  }
}
