package com.dream11.mysql.operation;

import com.dream11.mysql.service.RedeployService;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class Redeploy implements Operation {
  final RedeployService redeployService;

  public boolean execute() {
    redeployService.redeployService();
    return true;
  }
}
