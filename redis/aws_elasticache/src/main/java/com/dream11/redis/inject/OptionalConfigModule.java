package com.dream11.redis.inject;

import com.google.inject.AbstractModule;

import lombok.Builder;
import lombok.NonNull;

/**
 * This module is to be used in operations that require parameters which are not
 * part of deploy
 * config. Example update-node-type, update-nodegroup-count, update-replica-count operation
 *
 * @param <T> Custom config object. Example update-node-type config, update-nodegroup-count config, update-replica-count config etc
 */
@Builder
public class OptionalConfigModule<T> extends AbstractModule {

    @NonNull
    final T config;
    @NonNull
    final Class<T> clazz;

    @Override
    protected void configure() {
        bind(clazz).toInstance(this.config);
    }
}
