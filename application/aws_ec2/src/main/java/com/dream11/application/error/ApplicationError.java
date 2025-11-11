package com.dream11.application.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ApplicationError {
  CONSTRAINT_VIOLATION(ErrorCategory.USER_ERROR, "Constraint violation:[%s]"),
  INVALID_ARGUMENTS(
      ErrorCategory.ODIN_ERROR, "Please pass an operation as an argument. Available operations:%s"),
  NO_DOMAIN_FOUND_FOR_ROUTE(ErrorCategory.ODIN_ERROR, "No domain found for route:[%s]"),
  LCU_GET_CALL_FAILED(
      ErrorCategory.AWS_ERROR,
      "Failed to get provisioned capacity for load balancer:[%s]. Error:[%s %s]"),
  LCU_MODIFY_CALL_FAILED(
      ErrorCategory.AWS_ERROR,
      "Failed to modify provisioned capacity for load balancer:[%s]. Error:[%s %s]"),

  DUPLICATE_ARCHITECTURE_IN_AMI(
      ErrorCategory.USER_ERROR, "Duplicate architectures:[%s] found in AMIs list"),
  DUPLICATE_ARCHITECTURE_IN_INSTANCES(
      ErrorCategory.USER_ERROR, "Duplicate architectures:[%s] found in instances list"),
  INVALID_INSTANCE_ARCHITECTURE(
      ErrorCategory.USER_ERROR,
      "Architectures:[%s] specified in instances list is not found in AMIs list"),
  NO_INSTANCE_FOUND_FOR_ARCHITECTURE(
      ErrorCategory.USER_ERROR, "No instances found for architecture:[%s]"),
  UNHEALTHY_APPLICATION_TIMEOUT(
      ErrorCategory.USER_ERROR,
      "Timeout of:[%s minutes] exceeded while waiting for healthy targets. Please check logs of EC2 instances to see why health checks are failing. ASG: [%s]"),
  LCU_PROVISIONING_TIMEOUT(
      ErrorCategory.AWS_ERROR,
      "Timeout of:[%s minutes] exceeded while waiting for lcu provisioning"),
  TARGET_DRAIN_TIMEOUT(
      ErrorCategory.AWS_ERROR,
      "Timeout of:[%s minutes] exceeded while waiting for targets to drain"),
  MULTIPLE_ASG_WITH_NON_ZERO_CAPACITY(
      ErrorCategory.USER_ERROR,
      "There should be exactly one ASG with non zero capacity for non discoverable applications. Found asgs:[%s]"),
  NO_ASG_WITH_ZERO_CAPACITY(
      ErrorCategory.USER_ERROR, "There should be at least one ASG with zero capacity"),
  PASSIVE_STACK_NOT_FOUND(ErrorCategory.USER_ERROR, "Passive stack not found for stack:[%d]"),
  INVALID_ARN(ErrorCategory.ODIN_ERROR, "Invalid ARN:[%s]"),
  ABSOLUTE_CANARY_ANALYSIS_FAILED(
      ErrorCategory.USER_ERROR, "Canary analysis failed. Error count:[%f] > threshold:[%d]"),
  PERCENTAGE_CANARY_ANALYSIS_FAILED(
      ErrorCategory.USER_ERROR, "Canary analysis failed. Error percentage:[%f] > threshold:[%d]"),
  INVALID_ROUTE53_WEIGHT(
      ErrorCategory.USER_ERROR,
      "Invalid weight:[%d] for route 53 with identifier:[%s%s%s]. Allowed values are:%s"),
  NO_PASSIVE_RECORD_FOUND(
      ErrorCategory.USER_ERROR,
      "Route53s for stackId:[%s] are inconsistent. No records found with weight 0"),
  MULTIPLE_ACTIVE_RECORDS(
      ErrorCategory.ODIN_ERROR,
      "Route53s for stackId:[%s] are inconsistent. There should be exactly one route with non zero weight"),
  INCONSISTENT_ROUTE53(
      ErrorCategory.USER_ERROR,
      "Route53s for stackId:[%s] are inconsistent. Both public and private route should have same weight distribution"),
  ROUTE53_NOT_FOUND(ErrorCategory.ODIN_ERROR, "Route53 weight for identifier:[%s%s%s] not found"),
  INCORRECT_ACTIVE_ROUTE_FOUND(
      ErrorCategory.USER_ERROR,
      "Route53 with name:[%s] and identifier:[%s] have a DNS which is not present in state and weight is non zero. Please check for inconsistencies or contact administrator"),
  SSM_COMMAND_FAILED(ErrorCategory.ODIN_ERROR, "SSM command:[%s] failed. Status:[%s]"),
  OPERATION_NOT_ALLOWED_FOR_NON_DISCOVERABLE_APPLICATION(
      ErrorCategory.USER_ERROR, "Operation %s is not allowed for non discoverable applications"),
  INVALID_STACK_NUMBER(
      ErrorCategory.USER_ERROR, "Desired stacks:[%d] should be greater than current stacks:[%d]"),
  ODIN_ERROR(ErrorCategory.ODIN_ERROR, "%s"),
  UNHEALTHY_INSTANCES(
      ErrorCategory.USER_ERROR, "Healthy instance count for load balancer:[%s] is less than 1"),
  TEMPLATE_FILE_NOT_FOUND(ErrorCategory.ODIN_ERROR, "Template file:[%s] not found"),
  DEPLOY_CONFIG_NOT_FOUND_IN_STATE(
      ErrorCategory.ODIN_ERROR, "Deploy config not found in state. Cannot perform %s operation"),
  LOAD_BALANCER_DOES_NOT_EXIST(
      ErrorCategory.ODIN_ERROR, "Load balancer with identifier:[%s] does not exist"),
  HEALTHCHECK_CONFIG_NOT_FOUND(
      ErrorCategory.ODIN_ERROR, "No healthcheck config found while creating classic load balancer"),
  INVALID_HEALTHCHECK_CONFIG(
      ErrorCategory.USER_ERROR,
      "Health checks should be exactly same for all listeners for classic load balancer"),
  INVALID_DEPLOYMENT_STRATEGY(ErrorCategory.USER_ERROR, "Invalid deployment strategy:[%s]"),
  INVALID_MODE(ErrorCategory.USER_ERROR, "Invalid deployment strategy:[%s]"),
  INVALID_DEPLOYMENT_STACK(ErrorCategory.ODIN_ERROR, "Deployment stack must one of [%s, %s]"),
  INVALID_OPERATION(ErrorCategory.ODIN_ERROR, "Invalid operation:[%s]"),
  SERVICE_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No service with category:[%s] found"),
  ACCOUNT_NOT_FOUND(ErrorCategory.ODIN_ERROR, "No account with category:[%s] found"),
  CORRUPTED_STATE_FILE(ErrorCategory.ODIN_ERROR, "Corrupted state file"),
  INVALID_CLOUD_PROVIDER(
      ErrorCategory.ODIN_ERROR,
      "EC2 flavour cannot be invoked with %s cloud provider. Requires 'aws' cloud provider"),
  R53_SYNC(
      ErrorCategory.AWS_ERROR,
      "Timeout of:[%s minutes] exceeded while waiting for R53 change to become INSYNC");
  final ErrorCategory category;
  final String message;
}
