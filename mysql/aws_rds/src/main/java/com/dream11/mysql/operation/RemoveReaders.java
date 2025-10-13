package com.dream11.mysql.operation;

import com.dream11.mysql.service.RDSService;
import com.dream11.mysql.service.StateCorrectionService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class RemoveReaders implements Operation {
  @NonNull final RDSService rdsService;
  @NonNull final StateCorrectionService stateCorrectionService;

  @Override
  public void execute() {
    this.stateCorrectionService.correctState();
    this.rdsService.removeReaders();
  }
}
