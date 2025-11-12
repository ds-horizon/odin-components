package com.dream11.mysql.operation;

import com.dream11.mysql.service.RDSService;
import com.dream11.mysql.service.StateService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Undeploy implements Operation {
  @NonNull final RDSService rdsService;
  @NonNull final StateService stateService;

  @Override
  public void execute() {
    this.stateService.reconcileState();
    this.rdsService.undeploy();
  }
}
