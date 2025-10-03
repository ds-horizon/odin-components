package com.dream11.application.constant;

import java.time.Duration;
import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public final String COMPONENT_STATE_FILE = "state.json";
  public final String CONFIG = "CONFIG";
  public final String COMPONENT_METADATA = "COMPONENT_METADATA";
  public final String LOCAL = "LOCAL";
  public final Integer UUID_LENGTH = 4;
  public final String DOCKER_REGISTRY_SERVICE = "DOCKER_REGISTRY";
  public final String BASE_DIR = "/app";
  public final String DEPLOYMENT_TYPE = "local_kubernetes";
  public final UnaryOperator<String> APPLICATION_DIRECTORY =
      artifactName -> String.format("%s/%s", BASE_DIR, artifactName);
  public final String HELM_VALUES_FILE = "helm/values.yaml";
  public final String VALUES_FILE = "values.yaml";
  public final Duration WAIT_FOR_HEALHTY_TARGETS_DURATION = Duration.ofMinutes(5);
  public final String PACKER_FILE = "image.pkr.hcl";
  public final String PACKER_TEMPLATE_FILE = "image/image.pkr.hcl";
  public final String KUBERNETES_CATEGORY = "KUBERNETES";
}
