package com.dream11.application.state;

import com.dream11.application.config.user.DeployConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class State {
  int version;
  ImageState image;
  String releaseName;
  String deploymentNamespace;
  DeployConfig deployConfig;

  public void incrementVersion() {
    this.version++;
  }

  public void clearReleaseName() {
    this.releaseName = null;
  }
}
