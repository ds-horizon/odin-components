package com.dream11.mysql.inject;

import com.google.inject.AbstractModule;
import lombok.Builder;
import lombok.NonNull;

/**
 * This module is to be used in operations that require parameters which are not part of deploy
 * config. Example Rolling restart, update stack operation
 *
 * @param <T> Custom config object. Example Rolling restart config, update stack config etc
 */
@Builder
public class OptionalConfigModule<T> extends AbstractModule {

  @NonNull final T config;
  @NonNull final Class<T> clazz;

  @Override
  protected void configure() {
    bind(clazz).toInstance(this.config);
  }
}
