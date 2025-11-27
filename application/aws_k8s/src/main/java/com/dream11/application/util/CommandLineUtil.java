package com.dream11.application.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandLineUtil {
  @SneakyThrows
  public static CommandResult execute(String... cmd) {
    Process process = new ProcessBuilder().command(cmd).start();
    try (BufferedReader outStream =
            new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errStream =
            new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
      String output = outStream.lines().collect(Collectors.joining("\n"));
      String error = errStream.lines().collect(Collectors.joining("\n"));
      process.waitFor();
      return new CommandResult(process.exitValue(), output, error);
    }
  }

  @Getter
  @AllArgsConstructor
  public static class CommandResult {
    int exitCode;
    String stdOut;
    String stdErr;
  }
}
