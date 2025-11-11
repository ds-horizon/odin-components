package com.dream11.application;

import com.dream11.application.aws.AutoscalingGroupClient;
import com.dream11.application.aws.ClassicLoadBalancerClient;
import com.dream11.application.aws.CloudwatchClient;
import com.dream11.application.aws.EC2Client;
import com.dream11.application.aws.LaunchTemplateClient;
import com.dream11.application.aws.LoadBalancerClient;
import com.dream11.application.aws.Route53Client;
import com.dream11.application.aws.SystemsManagerClient;
import com.dream11.application.aws.TargetGroupClient;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.config.metadata.aws.AwsAccountData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.config.user.RevertConfig;
import com.dream11.application.config.user.RollingRestartConfig;
import com.dream11.application.config.user.UpdateStackConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.Operations;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.error.ErrorCategory;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.inject.AwsModule;
import com.dream11.application.inject.ConfigModule;
import com.dream11.application.inject.OptionalConfigModule;
import com.dream11.application.operation.AMITemplate;
import com.dream11.application.operation.Deploy;
import com.dream11.application.operation.Operation;
import com.dream11.application.operation.PassiveDownscale;
import com.dream11.application.operation.Redeploy;
import com.dream11.application.operation.Revert;
import com.dream11.application.operation.RollingRestart;
import com.dream11.application.operation.Scale;
import com.dream11.application.operation.Status;
import com.dream11.application.operation.Undeploy;
import com.dream11.application.operation.Update;
import com.dream11.application.operation.UpdateStack;
import com.dream11.application.state.State;
import com.dream11.application.util.ApplicationUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

@Slf4j
@RequiredArgsConstructor
public class Application {

  @Getter
  static final ObjectMapper objectMapper =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
          .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .withConfigOverride(
              ArrayNode.class, mutableConfigOverride -> mutableConfigOverride.setMergeable(false))
          .build();

  @Getter
  static final ExecutorService executorService =
      new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0L, TimeUnit.SECONDS, new SynchronousQueue<>());

  final String operationName;

  LoadBalancerClient loadBalancerClient;
  AutoscalingGroupClient autoscalingGroupClient;
  TargetGroupClient targetGroupClient;
  Route53Client route53Client;

  LaunchTemplateClient launchTemplateClient;

  ClassicLoadBalancerClient classicLoadBalancerClient;

  EC2Client ec2Client;
  CloudwatchClient cloudwatchClient;

  SystemsManagerClient systemsManagerClient;

  String config;
  ComponentMetadata componentMetadata;
  AwsAccountData awsAccountData;
  DeployConfig deployConfig;

  @Getter @Setter static State state;

  @SneakyThrows
  public static void main(String[] args) {
    // Setting error stream to null, to avoid library errors like guice, otherwise errors will be
    // printed twice in CLI
    System.setErr(new PrintStream(OutputStream.nullOutputStream()));
    try {
      if (args.length == 0) {
        throw new GenericApplicationException(
            ApplicationError.INVALID_ARGUMENTS, Arrays.toString(Operations.values()));
      }
      Application application = new Application(args[0]);
      application.start();
    } catch (Exception throwable) {
      Throwable rootCause = ApplicationUtil.getRootCause(throwable);
      if (rootCause instanceof GenericApplicationException) {
        log.error(rootCause.getMessage());
      } else if (rootCause instanceof AwsServiceException) {
        log.error("{}: {}", ErrorCategory.AWS_ERROR, rootCause.getMessage());
      } else {
        log.error("{}: {}", ErrorCategory.ODIN_ERROR, rootCause.getMessage());
      }
      log.debug("Error:", rootCause); // Logging stack trace in debug to avoid it being sent to CLI
      System.exit(1);
    }
  }

  void start() {
    this.addShutdownHook();
    this.readConfigFromEnvVariables();
    this.readState();
    this.initialiseAwsClients();
    this.executeOperation();
  }

  private void addShutdownHook() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.debug("Performing exit steps");
                  this.shutdown();
                }));
  }

  @SneakyThrows
  void readState() {
    File stateFile = new File(Constants.COMPONENT_STATE_FILE);
    if (stateFile.exists()) {
      log.debug("Reading state from file:[{}]", Constants.COMPONENT_STATE_FILE);
      String stateContent = FileUtils.readFileToString(stateFile, Charset.defaultCharset());
      log.debug("State content:[{}]", stateContent);
      try {
        State previousState = Application.getObjectMapper().readValue(stateContent, State.class);
        log.debug("Loaded state:[{}]", previousState);
        Application.setState(previousState);
      } catch (JsonProcessingException ex) {
        throw new GenericApplicationException(ApplicationError.CORRUPTED_STATE_FILE);
      }
    } else {
      log.warn("No state file found.");
      Application.setState(State.builder().build());
    }
  }

  @SneakyThrows
  void executeOperation() {
    Class<? extends Operation> operationClass;
    List<Module> modules = new ArrayList<>();
    // Read current deploy config from state. Will be overridden in operations if needed
    this.deployConfig = Application.getState().getDeployConfig();
    operationClass =
        switch (Operations.fromValue(this.operationName)) {
          case DEPLOY -> {
            // Override the deploy config with user provided config
            this.deployConfig =
                Application.getObjectMapper().readValue(this.config, DeployConfig.class);
            yield Deploy.class;
          }
          case UPDATE_STACK -> {
            this.deployConfig = this.deployConfig.mergeWith(this.config);
            UpdateStackConfig config =
                Application.getObjectMapper().readValue(this.config, UpdateStackConfig.class);
            modules.add(
                OptionalConfigModule.<UpdateStackConfig>builder()
                    .clazz(UpdateStackConfig.class)
                    .config(config)
                    .build());
            yield UpdateStack.class;
          }
          case AMI_TEMPLATE -> {
            if (Objects.nonNull(this.deployConfig)) {
              // When performing redeploy operation
              this.deployConfig = this.deployConfig.mergeWith(this.config);
            } else {
              // When performing deploy operation
              this.deployConfig =
                  Application.getObjectMapper().readValue(this.config, DeployConfig.class);
            }
            yield AMITemplate.class;
          }
          case UNDEPLOY -> Undeploy.class;
          case ROLLING_RESTART -> {
            RollingRestartConfig config =
                Application.getObjectMapper().readValue(this.config, RollingRestartConfig.class);
            log.debug("Rolling restart config:[{}]", config);
            // Add rolling restart config to guice modules
            modules.add(
                OptionalConfigModule.<RollingRestartConfig>builder()
                    .clazz(RollingRestartConfig.class)
                    .config(config)
                    .build());
            yield RollingRestart.class;
          }
          case SCALE -> {
            this.deployConfig = this.deployConfig.mergeWith(this.config);
            yield Scale.class;
          }
          case PASSIVE_DOWNSCALE -> PassiveDownscale.class;
          case REVERT -> {
            RevertConfig config =
                Application.getObjectMapper().readValue(this.config, RevertConfig.class);
            log.debug("Revert config:[{}]", config);
            // Add revert config to guice modules
            modules.add(
                OptionalConfigModule.<RevertConfig>builder()
                    .clazz(RevertConfig.class)
                    .config(config)
                    .build());
            yield Revert.class;
          }
          case REDEPLOY -> {
            if (Objects.nonNull(this.deployConfig)) {
              this.deployConfig = this.deployConfig.mergeWith(this.config);
            } else {
              throw new GenericApplicationException(
                  ApplicationError.DEPLOY_CONFIG_NOT_FOUND_IN_STATE, "redeploy");
            }
            yield Redeploy.class;
          }
          case STATUS -> Status.class;
          case UPDATE_ASG -> {
            this.deployConfig = this.deployConfig.mergeWith(this.config);
            yield Update.class;
          }
        };
    modules.addAll(this.getGuiceModules());
    Operation operation = this.initializeGuiceModules(modules).getInstance(operationClass);
    log.debug("Executing operation:[{}]", Operations.fromValue(this.operationName));
    if (Operations.fromValue(this.operationName).equals(Operations.UNDEPLOY)) {
      log.info("Deleting all created resources");
    } else {
      log.debug("Desired deployment config: {}", this.deployConfig.toString());
    }
    if (operation.execute()) {
      Application.getState()
          .setDeployConfig(
              this.deployConfig); // Update deployment config in state after successful execution
    }
  }

  private void shutdown() {
    executorService.shutdown();
    this.writeState(); // Write state to file in all cases
  }

  @SneakyThrows
  void writeState() {
    if (Objects.nonNull(Application.getState())) {
      Application.getState().incrementVersion();
      log.debug("Final state: {}", Application.getState());
      log.debug("Writing state");
      FileUtils.writeStringToFile(
          new File(Constants.COMPONENT_STATE_FILE),
          Application.getObjectMapper().writeValueAsString(Application.getState()),
          Charset.defaultCharset());
    } else {
      log.warn("State is null. Not writing to file");
    }
  }

  @SneakyThrows
  void readConfigFromEnvVariables() {
    this.config = System.getenv(Constants.CONFIG);
    this.componentMetadata =
        Application.getObjectMapper()
            .readValue(System.getenv(Constants.COMPONENT_METADATA), ComponentMetadata.class);
    this.awsAccountData =
        Application.getObjectMapper()
            .convertValue(
                this.componentMetadata.getCloudProviderDetails().getAccount().getData(),
                AwsAccountData.class);
  }

  void initialiseAwsClients() {
    String cloudProvider =
        this.componentMetadata.getCloudProviderDetails().getAccount().getProvider();
    if (!cloudProvider.equalsIgnoreCase(Constants.AWS)) {
      throw new GenericApplicationException(ApplicationError.INVALID_CLOUD_PROVIDER, cloudProvider);
    }

    RetryStrategy retryStrategy =
        AwsRetryStrategy.standardRetryStrategy().toBuilder()
            .maxAttempts(Constants.MAX_ATTEMPTS)
            .throttlingBackoffStrategy(
                BackoffStrategy.exponentialDelayHalfJitter(
                    Duration.ofSeconds(Constants.RETRY_DELAY),
                    Duration.ofSeconds(Constants.RETRY_MAX_BACKOFF)))
            .build();

    // Configure HTTP client for AWS
    SdkHttpClient httpClient =
        ApacheHttpClient.builder().socketTimeout(Constants.AWS_API_READ_TIMEOUT).build();
    String region = this.awsAccountData.getRegion();
    this.ec2Client = new EC2Client(region, retryStrategy, httpClient);

    // TODO use http client for all clients
    this.loadBalancerClient = new LoadBalancerClient(region, retryStrategy);
    this.classicLoadBalancerClient = new ClassicLoadBalancerClient(region, retryStrategy);
    this.autoscalingGroupClient = new AutoscalingGroupClient(region, retryStrategy);
    this.targetGroupClient = new TargetGroupClient(region, retryStrategy);
    this.route53Client = new Route53Client(region, retryStrategy);
    this.launchTemplateClient = new LaunchTemplateClient(region, retryStrategy);
    this.cloudwatchClient = new CloudwatchClient(region, retryStrategy);
    this.systemsManagerClient = new SystemsManagerClient(region, retryStrategy);
  }

  private Injector initializeGuiceModules(List<Module> modules) {
    return Guice.createInjector(modules);
  }

  private List<Module> getGuiceModules() {
    return new ArrayList<>(
        List.of(
            AwsModule.builder()
                .classicLoadBalancerClient(this.classicLoadBalancerClient)
                .loadBalancerClient(this.loadBalancerClient)
                .targetGroupClient(this.targetGroupClient)
                .route53Client(this.route53Client)
                .launchTemplateClient(this.launchTemplateClient)
                .autoscalingGroupClient(this.autoscalingGroupClient)
                .ec2Client(this.ec2Client)
                .cloudwatchClient(this.cloudwatchClient)
                .systemsManagerClient(this.systemsManagerClient)
                .build(),
            ConfigModule.builder()
                .componentMetadata(this.componentMetadata)
                .deployConfig(this.deployConfig)
                .build()));
  }
}
