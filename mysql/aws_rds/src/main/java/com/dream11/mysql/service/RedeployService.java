package com.dream11.mysql.service;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RedeployService {
  public void redeployService() {
    log.info("Redeploying the service");
  }
}
