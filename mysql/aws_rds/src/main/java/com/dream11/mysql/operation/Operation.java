package com.dream11.mysql.operation;

@FunctionalInterface
public interface Operation {
  boolean
      execute(); // Return value indicates whether deploy config in  state needs to be updated after
  // successful execution
}
