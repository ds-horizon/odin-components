package com.dream11.mysql.operation;

import com.dream11.mysql.service.DeployService;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Deploy implements Operation {
  final DeployService deployService;

  public boolean execute() {
    deployService.deployService();
    return true;
  }
}
