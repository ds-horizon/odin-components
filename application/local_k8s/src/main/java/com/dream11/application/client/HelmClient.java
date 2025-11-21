package com.dream11.application.client;

import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.exception.HelmReleaseNotFoundException;
import com.dream11.application.util.CommandLineUtil;

public class HelmClient {

  public void upgrade(
      String releaseName,
      String namespace,
      String chartPath,
      String valuesPath,
      String waitTimeout) {
    // Install the chart
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute(
            "helm",
            "upgrade",
            "--install",
            releaseName,
            chartPath,
            "--namespace",
            namespace,
            "--wait",
            "--timeout",
            waitTimeout,
            "--values",
            valuesPath);
    if (result.getExitCode() != 0) {
      throw new GenericApplicationException(
          ApplicationError.HELM_CHART_UPGRADE_FAILED, result.getStdErr());
    }
  }

  public void uninstall(String releaseName, String namespace) {
    // Uninstall the chart
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute("helm", "uninstall", releaseName, "--namespace", namespace);
    if (result.getExitCode() != 0) {
      if (result.getStdErr().contains("release: not found")) {
        throw new HelmReleaseNotFoundException(result.getStdErr());
      }
      throw new GenericApplicationException(
          ApplicationError.HELM_CHART_UNINSTALL_FAILED, result.getStdErr());
    }
  }
}
