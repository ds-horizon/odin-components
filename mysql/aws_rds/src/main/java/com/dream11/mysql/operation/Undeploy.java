package com.dream11.mysql.operation;

import com.dream11.mysql.service.UndeployService;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Undeploy implements Operation {
  final UndeployService undeployService;

  @Override
  public boolean execute() {
    undeployService.undeployService();
    return true;
  }
}
