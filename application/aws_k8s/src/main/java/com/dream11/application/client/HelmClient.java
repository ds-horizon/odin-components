package com.dream11.application.client;

import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.util.CommandLineUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelmClient {
  public void upgradeInstall(
      String releaseName, String namespace, String chartPath, String valuesPath) {
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute(
            "helm",
            "upgrade",
            releaseName,
            chartPath,
            "--namespace",
            namespace,
            "--values",
            valuesPath,
            "--install");
    if (result.getExitCode() != 0) {
      throw new GenericApplicationException(
          ApplicationError.HELM_CHART_UPGRADE_FAILED, result.getStdErr());
    }
  }

  public void uninstall(String releaseName, String namespace) {
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute("helm", "uninstall", releaseName, "--namespace", namespace);
    if (result.getExitCode() != 0) {
      if (result.getStdErr().contains("release: not found")) {
        log.warn("Helm release not found: {}", releaseName);
        return;
      }
      throw new GenericApplicationException(
          ApplicationError.HELM_CHART_UNINSTALL_FAILED, result.getStdErr());
    }
  }

  public boolean releaseExists(String releaseName, String namespace) {
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute("helm", "status", releaseName, "--namespace", namespace);
    return result.getExitCode() == 0;
  }

  public void rollback(String releaseName, String namespace) {
    CommandLineUtil.CommandResult result =
        CommandLineUtil.execute("helm", "rollback", releaseName, "--namespace", namespace);
    if (result.getExitCode() != 0) {
      throw new GenericApplicationException(
          ApplicationError.HELM_ROLLBACK_FAILED, result.getStdErr());
    }
  }
}
