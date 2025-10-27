package com.dream11.application.constant;

import com.dream11.application.util.ApplicationUtil;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public final Integer MAX_RETRIES = 10;
  public final Integer RETRY_DELAY = 3;
  public final Duration AWS_API_READ_TIMEOUT = Duration.ofSeconds(120);
  public final Integer RETRY_MAX_BACKOFF = 120;

  public final Duration WAIT_FOR_INSTANCES_TO_DRAIN_DURATION = Duration.ofMinutes(10);
  public final Duration WAIT_FOR_INITIAL_HEALHTY_INSTANCES_DURATION = Duration.ofMinutes(10);
  public final Duration WAIT_FOR_TOTAL_HEALHTY_INSTANCES_DURATION = Duration.ofMinutes(20);

  public final Duration WAIT_FOR_LCU_PROVISIONING_DURATION = Duration.ofMinutes(15);
  public final Duration DELAY_FOR_MAKING_NEXT_REQUEST = Duration.ofSeconds(10);
  public final Duration DELAY_FOR_CLOUD_METRICS_STABILIZATION = Duration.ofSeconds(90);

  public final String CLB_HEALTHY_INSTANCE_STATE = "InService";
  public final String AWS = "aws";

  public final String CONFIG = "CONFIG";
  public final String COMPONENT_METADATA = "ODIN_COMPONENT_METADATA";

  public final String COMPONENT_STATE_FILE = "state.json";

  public final Integer LB_RANDOM_ID_LENGTH = 16;

  public final Integer ASG_RANDOM_ID_LENGTH = 5;

  public final Integer AMI_RANDOM_ID_LENGTH = 4;
  public final Long ROUTE_53_TTL = 60L;

  public final String NETWORK_CATEGORY = "NETWORK";

  public final String DISCOVERY_CATEGORY = "DISCOVERY";
  public final String EC2_CATEGORY = "VM";
  public final String ARTIFACTORY_ACCOUNT_CATEGORY = "ARTIFACTORY";

  public final String ARTIFACTORY_STORAGE_CATEGORY = "STORAGE";
  public final String PUBLIC = "public";
  public final String PRIVATE = "private";

  public final Map<String, String> COMPONENT_TAGS =
      Map.of(
          "component:application:flavour",
          "aws_ec2",
          "component:application:version",
          ApplicationUtil.getProjectVersion());

  public final String PACKER_TEMPLATE_FILE = "ami/ami.pkr.hcl";
  public final String USERDATA_TEMPLATE_FILE = "userdata/userdata.sh.tpl";

  public final UnaryOperator<String> DOWNLOAD_ARTIFACT_TEMPLATE_FILE =
      provider -> String.format("artifactory/%s_download.sh", provider);
  public final String PACKER_FILE = "ami.pkr.hcl";
  public final String TARGET_GROUP_ARN_ENV_VARIABLE = "TARGET_GROUP_ARN";
  public final String LOAD_BALANCER_NAME_ENV_VARIABLE = "LOADBALANCER_NAME";

  public final String DEPLOYMENT_STACK_ENV_VARIABLE = "DEPLOYMENT_STACK";

  public final Character BLUE_DEPLOYMENT_STACK_IDENTIFIER = 'b';
  public final Character GREEN_DEPLOYMENT_STACK_IDENTIFIER = 'g';
  public final String BLUE_DEPLOYMENT_STACK_NAME = "BLUE";
  public final String GREEN_DEPLOYMENT_STACK_NAME = "GREEN";

  public final Character INTERNAL_IDENTIFIER = 'i';

  public final Character EXTERNAL_IDENTIFIER = 'e';
  public final String LCU_PROVISIONED_STATUS = "provisioned";

  public final String NAME_TAG = "Name";
  public final String ARTIFACT_NAME_TAG = "component:application:artifact_name";
  public final String ARTIFACT_VERSION_TAG = "component:application:artifact_version";
  public final String DEPLOYMENT_STRATEGY_TAG = "component:application:deployment_strategy";
  public final String APPLICATION_DEPLOYMENT_STACK_TAG = "component:application:deployment_stack";
  public final String DEPLOYMENT_STACK_TAG = "deployment_stack";
  public final String SHEBANG_COMMAND = "#!/usr/bin/env bash";
  public final UnaryOperator<String> RESTART_SERVICE_COMMAND =
      componentName ->
          String.format(
              """
              sudo systemctl restart %s --no-block
              sleep 12
          """,
              componentName);
  public final Map<Protocol, String> HEALTHCHECK_TEMPLATE_FILES =
      Map.of(
          Protocol.HTTP, "healthcheck/http.sh",
          Protocol.HTTPS, "healthcheck/http.sh",
          Protocol.TCP, "healthcheck/tcp.sh",
          Protocol.GRPC, "healthcheck/grpc.sh");

  public final Long ROLLING_RESTART_EXECUTION_TIMEOUT = 180L;
  public final String DEREGISTER_TARGETS_COMMAND = "manage_targets deregister 5";
  public final Integer TARGET_REGISTRATION_WAIT_SECONDS = 15;

  public static final String MANAGE_TARGETS_TEMPLATE_FILE = "ssm/manage_targets.sh";
  public final Integer MIN_DELIVERY_TIMEOUT_IN_SEC = 1200;
  public final List<String> ASG_NOTIFICATION_TYPES =
      List.of(
          "autoscaling:EC2_INSTANCE_LAUNCH",
          "autoscaling:EC2_INSTANCE_LAUNCH_ERROR",
          "autoscaling:EC2_INSTANCE_TERMINATE",
          "autoscaling:EC2_INSTANCE_TERMINATE_ERROR");

  public final String BASE_DIR = "/var/www";
  public final String DEPLOYMENT_TYPE = "aws_ec2";
  public final UnaryOperator<String> APPLICATION_DIRECTORY =
      artifactName -> String.format("%s/%s", BASE_DIR, artifactName);

  public final String PROJECT_PROPERTIES = "project.properties";
  public final String ARCHITECTURE_IMAGE_FILTER = "architecture";

  public final Integer AMI_HASH_LENGTH = 16;
  public final Duration WAIT_FOR_R53_SYNC = Duration.ofMinutes(5);

  public final String LATEST = "$Latest";
}
