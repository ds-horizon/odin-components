package com.dream11.application.inject;

import com.dream11.application.config.Config;
import com.google.inject.AbstractModule;
import lombok.Builder;
import lombok.NonNull;

/**
 * This module is to be used in operations that require parameters which are not part of deploy
 * config. Example Rolling restart, revert operation
 *
 * @param <T> Custom config object. Example Rolling restart config, revert config etc
 */
@Builder
public class OptionalConfigModule<T extends Config> extends AbstractModule {

  @NonNull final T config;
  @NonNull final Class<T> clazz;

  @Override
  protected void configure() {
    this.config.validate();
    bind(clazz).toInstance(this.config);
  }
}
