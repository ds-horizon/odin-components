package com.dream11.application.constant;

import com.dream11.application.util.ApplicationUtil;
import java.time.Duration;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public final String COMPONENT_STATE_FILE = "state.json";
  public final String CONFIG = "CONFIG";
  public final String COMPONENT_METADATA = "COMPONENT_METADATA";
  public final String ARTIFACTORY_CATEGORY = "ARTIFACTORY";
  public final String DOCKER_REGISTRY_SERVICE = "DOCKER_REGISTRY";
  public final String CLOUD_CATEGORY = "CLOUD";
  public final String KUBERNETES = "KUBERNETES";
  public final String NETWORK = "NETWORK";
  public final Integer UUID_LENGTH = 8;
  public final String BASE_DIR = "/app";
  public final String DEPLOYMENT_TYPE = "aws_container";
  public final UnaryOperator<String> APPLICATION_DIRECTORY =
      artifactName -> String.format("%s/%s", BASE_DIR, artifactName);
  public final String PACKER_FILE = "image.pkr.hcl";
  public final String PACKER_TEMPLATE_FILE = "image/image.pkr.hcl";
  public final String INTERNAL = "internal";
  public final String EXTERNAL = "external";
  public final String PUBLIC = "public";
  public final String PRIVATE = "private";
  public final String HELM_VALUES_FILE = "helm/values.yaml";
  public final Duration WAIT_FOR_INITIAL_HEALHTY_TARGETS_DURATION = Duration.ofMinutes(10);
  public final Duration WAIT_FOR_HEALTHY_TARGETS_DURATION = Duration.ofMinutes(10);

  public final Map<String, String> COMPONENT_TAGS =
      Map.of(
          "component.application.flavour",
          "aws_container",
          "component.application.version",
          ApplicationUtil.getProjectVersion());
  public final String PROJECT_PROPERTIES = "project.properties";
}
