package com.dream11.application.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.Command;
import software.amazon.awssdk.services.ssm.model.CommandInvocation;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;
import software.amazon.awssdk.services.ssm.model.ListCommandInvocationsRequest;
import software.amazon.awssdk.services.ssm.model.ListCommandInvocationsResponse;
import software.amazon.awssdk.services.ssm.model.SendCommandRequest;
import software.amazon.awssdk.services.ssm.model.Target;

@Slf4j
public class SystemsManagerClient {

  final SsmClient ssmClient;

  public SystemsManagerClient(String region, RetryStrategy retryStrategy) {
    this.ssmClient =
        SsmClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryStrategy(retryStrategy))
            .build();
  }

  private Command sendShellCommand(
      String comment,
      String maxConcurrency,
      String maxErrors,
      Long executionTimeout,
      Integer timeout,
      List<String> commands,
      Consumer<SendCommandRequest.Builder> mutator) {

    // Configure the SendCommandRequest using a Consumer
    return this.ssmClient
        .sendCommand(
            builder ->
                builder
                    .comment(comment)
                    .documentName("AWS-RunShellScript")
                    .documentVersion("$DEFAULT")
                    .maxConcurrency(maxConcurrency)
                    .maxErrors(maxErrors)
                    .timeoutSeconds(timeout)
                    .parameters(
                        Map.of(
                            "executionTimeout",
                            List.of(executionTimeout.toString()),
                            "commands",
                            commands))
                    .applyMutation(mutator))
        .command();
  }

  public Command sendShellCommandWithTags(
      String comment,
      String maxConcurrency,
      String maxErrors,
      Long executionTimeout,
      Integer timeout,
      String tagKey,
      List<String> tagValues,
      List<String> commands) {

    return this.sendShellCommand(
        comment,
        maxConcurrency,
        maxErrors,
        executionTimeout,
        timeout,
        commands,
        builder ->
            builder.targets(
                Target.builder().key(String.format("tag:%s", tagKey)).values(tagValues).build()));
  }

  public GetCommandInvocationResponse getCommandStatus(String commandId, String instanceId) {
    return this.ssmClient.getCommandInvocation(
        request -> request.commandId(commandId).instanceId(instanceId));
  }

  public List<CommandInvocation> listCommandInvocations(String commandId) {
    List<CommandInvocation> commandInvocations = new ArrayList<>();
    String nextToken = null;
    while (true) {
      ListCommandInvocationsRequest.Builder builder =
          ListCommandInvocationsRequest.builder().commandId(commandId);
      if (Objects.nonNull(nextToken)) {
        builder.nextToken(nextToken);
      }

      ListCommandInvocationsResponse response =
          this.ssmClient.listCommandInvocations(builder.build());
      commandInvocations.addAll(response.commandInvocations());
      if (Objects.isNull(response.nextToken())) {
        break;
      }
      nextToken = response.nextToken();
    }

    return commandInvocations;
  }

  public Command getCommand(String commandId) {
    return this.ssmClient.listCommands(request -> request.commandId(commandId)).commands().get(0);
  }
}
