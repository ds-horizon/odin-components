package com.dream11.application.state;

import com.dream11.application.config.DeployConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class State {
  @Builder.Default private int version = 0;
  private DeployConfig deployConfig;
  private DeploymentState deploymentState;
  private ImageState image;
  private String deploymentNamespace; // Stores the namespace for the deployment
  private boolean secretExist; // Stores whether image pull secret has been created

  public void incrementVersion() {
    this.version++;
  }
}
