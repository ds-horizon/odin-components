package com.dream11.application.service;

import com.dream11.application.aws.SystemsManagerClient;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.constant.Constants;
import com.dream11.application.entity.SSMCommand;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ssm.model.Command;
import software.amazon.awssdk.services.ssm.model.CommandInvocation;
import software.amazon.awssdk.services.ssm.model.CommandStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class SystemsManagerService {

  @NonNull final SystemsManagerClient systemsManagerClient;
  @NonNull final AutoscalingGroupService autoscalingGroupService;
  @NonNull final ComponentMetadata componentMetadata;

  public Command sendShellCommandWithTags(
      String comment,
      String maxConcurrency,
      String maxErrors,
      Long executionTimeout,
      Integer deliveryTimeout,
      String tagKey,
      List<String> tagValues,
      List<String> commands) {
    return this.systemsManagerClient.sendShellCommandWithTags(
        comment,
        maxConcurrency,
        maxErrors,
        executionTimeout,
        deliveryTimeout,
        tagKey,
        tagValues,
        commands);
  }

  @SneakyThrows
  public boolean checkCommandStatus(String commandId, long targetCount) {

    final String SUCCESS = "Success";
    final String FAILED = "Failed";
    final String DELIVERY_TIMED_OUT = "DeliveryTimedOut";
    final String CANCELLED = "Cancelled";
    final String TERMINATED = "Terminated";
    final String INVALID_PLATFORM = "InvalidPlatform";
    final String ACCESS_DENIED = "AccessDenied";

    Command command = this.systemsManagerClient.getCommand(commandId);

    long completedCount;

    while (command.status().equals(CommandStatus.IN_PROGRESS)
        || command.status().equals(CommandStatus.PENDING)
        || command.status().equals(CommandStatus.CANCELLING)) {

      List<CommandInvocation> commandInvocations =
          this.systemsManagerClient.listCommandInvocations(commandId);

      completedCount =
          getCommandInvocationCountByStatus(
              List.of(
                  SUCCESS,
                  FAILED,
                  DELIVERY_TIMED_OUT,
                  CANCELLED,
                  TERMINATED,
                  INVALID_PLATFORM,
                  ACCESS_DENIED),
              commandInvocations);

      this.logCommand(commandId, Math.max(targetCount, command.targetCount()), commandInvocations);

      if (completedCount >= Math.max(targetCount, command.targetCount())) {
        break;
      }

      // Command is in progress, check status after sometime
      Thread.sleep(Constants.DELAY_FOR_MAKING_NEXT_REQUEST.toMillis());
      command = this.systemsManagerClient.getCommand(commandId);
    }

    command = this.systemsManagerClient.getCommand(commandId);
    List<CommandInvocation> commandInvocations =
        this.systemsManagerClient.listCommandInvocations(commandId);

    this.logCommand(commandId, Math.max(targetCount, command.targetCount()), commandInvocations);
    long terminatedCount =
        getCommandInvocationCountByStatus(
            List.of(DELIVERY_TIMED_OUT, CANCELLED, TERMINATED, INVALID_PLATFORM, ACCESS_DENIED),
            commandInvocations);

    if (command.status().equals(CommandStatus.SUCCESS)
        || (command.status().equals(CommandStatus.IN_PROGRESS) && terminatedCount == 0)) {
      return true;
    }

    List<String> erroredInstances =
        commandInvocations.stream()
            .filter(commandInvocation -> commandInvocation.statusDetails().equals(FAILED))
            .map(CommandInvocation::instanceId)
            .toList();

    erroredInstances.forEach(
        erroredInstance -> {
          GetCommandInvocationResponse response =
              this.systemsManagerClient.getCommandStatus(commandId, erroredInstance);
          log.error(
              "Status on instance:[{}] is [{}] with error:[{}]",
              erroredInstance,
              response.statusDetails(),
              response.standardErrorContent());
        });
    throw new GenericApplicationException(
        ApplicationError.SSM_COMMAND_FAILED,
        commandId,
        this.getCommandInvocationStatus(commandInvocations));
  }

  private long getCommandInvocationCountByStatus(
      List<String> status, List<CommandInvocation> commandInvocations) {
    return commandInvocations.stream()
        .filter(commandInvocation -> status.contains(commandInvocation.statusDetails()))
        .count();
  }

  private void logCommand(
      String commandId, long targetCount, List<CommandInvocation> commandInvocations) {
    log.info(
        "Command id:[{}], Targets:[{}], Status:[{}]",
        commandId,
        targetCount,
        this.getCommandInvocationStatus(commandInvocations));
  }

  private Map<String, Long> getCommandInvocationStatus(List<CommandInvocation> commandInvocations) {
    return commandInvocations.stream()
        .map(CommandInvocation::statusDetails)
        .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
  }

  public boolean executeAndWait(String asgName, SSMCommand command) {
    long targetCount = this.autoscalingGroupService.getInServiceInstances(asgName);

    if (targetCount == 0) {
      log.info("No instances found in autoscaling group:[{}]. Skipping.", asgName);
      return true;
    }
    // Send command
    long concurrentInstancesSize =
        (long) Math.ceil((double) targetCount * command.getBatchSizePercentage() / 100);

    String concurrency = String.format("%d", concurrentInstancesSize);

    String maxErrors =
        String.format(
            "%d",
            (long) Math.ceil((double) targetCount * command.getErrorTolerancePercentage() / 100));

    int deliveryTimeout =
        Math.max(
            concurrentInstancesSize > 0
                ? (int) ((targetCount * 25) / Math.min(100, concurrentInstancesSize))
                : Constants.MIN_DELIVERY_TIMEOUT_IN_SEC,
            Constants.MIN_DELIVERY_TIMEOUT_IN_SEC);

    Command cmd =
        this.sendShellCommandWithTags(
            String.format("%s-%s", command.getDescription(), asgName),
            concurrency,
            maxErrors,
            Constants.ROLLING_RESTART_EXECUTION_TIMEOUT,
            deliveryTimeout,
            Constants.NAME_TAG,
            List.of(asgName),
            command.getCommands());
    log.info(
        "Performing {} for autoscaling group:[{}] with targetCount:[{}] using SSM. SSM command id:[{}]",
        command.getDescription(),
        asgName,
        targetCount,
        cmd.commandId());
    // Check for status
    return this.checkCommandStatus(cmd.commandId(), targetCount);
  }
}
