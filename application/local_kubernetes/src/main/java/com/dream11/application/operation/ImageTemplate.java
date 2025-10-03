package com.dream11.application.operation;

import com.dream11.application.service.ImageService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ImageTemplate implements Operation {

  @NonNull final ImageService imageService;

  @Override
  public boolean execute() {
    this.imageService.createImageIfNotExist();
    return false;
  }
}
