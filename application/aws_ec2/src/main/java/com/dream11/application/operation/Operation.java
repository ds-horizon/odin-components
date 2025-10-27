package com.dream11.application.operation;

@FunctionalInterface
public interface Operation {
  // Return value indicates whether deploy config in state needs to be updated after successful
  // execution
  boolean execute();
}
