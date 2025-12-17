package com.dream11.application;

import com.dream11.application.client.DockerClient;
import com.dream11.application.client.HelmClient;
import com.dream11.application.client.K8sClient;
import com.dream11.application.config.ComponentMetadata;
import com.dream11.application.config.DeployConfig;
import com.dream11.application.config.aws.AwsAccountData;
import com.dream11.application.config.aws.DockerRegistryData;
import com.dream11.application.config.aws.EKSData;
import com.dream11.application.config.aws.VPCData;
import com.dream11.application.config.user.RollingRestartConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.Operations;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.error.ErrorCategory;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.inject.AwsModule;
import com.dream11.application.inject.ConfigModule;
import com.dream11.application.inject.OptionalConfigModule;
import com.dream11.application.operation.CreateNamespace;
import com.dream11.application.operation.Deploy;
import com.dream11.application.operation.ImageTemplate;
import com.dream11.application.operation.Operation;
import com.dream11.application.operation.Redeploy;
import com.dream11.application.operation.Revert;
import com.dream11.application.operation.RollingRestart;
import com.dream11.application.operation.Scale;
import com.dream11.application.operation.Status;
import com.dream11.application.operation.Undeploy;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@RequiredArgsConstructor
public class Application {

  @Getter
  static final ObjectMapper objectMapper =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
          .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .withConfigOverride(
              ArrayNode.class, mutableConfigOverride -> mutableConfigOverride.setMergeable(false))
          .build();

  final String operationName;

  @Getter @Setter static State state;

  String config;
  ComponentMetadata componentMetadata;
  DockerRegistryData dockerRegistryData;
  AwsAccountData awsAccountData;
  EKSData eksData;
  VPCData vpcData;
  DeployConfig deployConfig;
  DockerClient dockerClient;
  K8sClient k8sClient;
  HelmClient helmClient;

  @SneakyThrows
  public static void main(String[] args) {
    System.setErr(new PrintStream(OutputStream.nullOutputStream()));
    try {
      if (args.length == 0) {
        throw new GenericApplicationException(
            ApplicationError.INVALID_ARGUMENTS, Arrays.toString(Operations.values()));
      }
      Application application = new Application(args[0]);
      application.start();
    } catch (Exception throwable) {
      log.error("Error: {}", throwable.getMessage());
      System.exit(1);
    }
  }

  void start() {
    this.addShutdownHook();
    this.readConfigFromEnvVariables();
    this.readState();
    this.initialiseClients();
    try {
      this.executeOperation();
    } finally {
      this.closeClients();
    }
  }

  private void addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  @SneakyThrows
  void readState() {
    File stateFile = new File(Constants.COMPONENT_STATE_FILE);
    if (stateFile.exists()) {
      String stateContent = FileUtils.readFileToString(stateFile, Charset.defaultCharset());
      try {
        State previousState = Application.getObjectMapper().readValue(stateContent, State.class);
        Application.setState(previousState);
      } catch (JsonProcessingException ex) {
        Throwable rootCause = ApplicationUtil.getRootCause(ex);
        if (rootCause instanceof GenericApplicationException) {
          log.error(rootCause.getMessage());
        } else {
          log.error("{}: {}", ErrorCategory.ODIN_ERROR, rootCause.getMessage());
        }
        log.debug(
            "Error:", rootCause); // Logging stack trace in debug to avoid it being sent to CLI
        System.exit(1);
      }
    } else {
      log.warn("Component state file not found, initializing with empty state");
      Application.setState(State.builder().build());
    }
  }

  @SneakyThrows
  void executeOperation() {
    Class<? extends Operation> operationClass;
    List<Module> modules = new ArrayList<>();

    this.deployConfig = Application.getState().getDeployConfig();
    log.info("Executing operation:{}", this.operationName);
    operationClass =
        switch (Operations.fromValue(this.operationName)) {
          case DEPLOY -> {
            // Parse config for new deployment
            this.deployConfig =
                Application.getObjectMapper().readValue(this.config, DeployConfig.class);
            yield Deploy.class;
          }
          case REDEPLOY -> {
            if (Objects.nonNull(this.deployConfig)) {
              this.deployConfig = this.deployConfig.mergeWith(this.config);
            } else {
              throw new GenericApplicationException(
                  ApplicationError.DEPLOY_CONFIG_NOT_FOUND_IN_STATE);
            }
            yield Redeploy.class;
          }
          case IMAGE_TEMPLATE -> {
            if (Objects.nonNull(this.deployConfig)) {
              // When performing redeploy operation
              this.deployConfig = this.deployConfig.mergeWith(this.config);
            } else {
              // When performing deploy operation
              this.deployConfig =
                  Application.getObjectMapper().readValue(this.config, DeployConfig.class);
            }
            yield ImageTemplate.class;
          }
          case CREATE_NAMESPACE -> CreateNamespace.class;
          case UNDEPLOY -> {
            this.deployConfig = null;
            yield Undeploy.class;
          }
          case SCALE -> {
            this.deployConfig = this.deployConfig.mergeWith(this.config);
            yield Scale.class;
          }
          case REVERT -> Revert.class;
          case ROLLING_RESTART -> {
            RollingRestartConfig rollingRestartConfig =
                Application.getObjectMapper().readValue(this.config, RollingRestartConfig.class);
            log.info("Rolling restart config:[{}]", config);
            // Add rolling restart config to guice modules
            modules.add(
                OptionalConfigModule.<RollingRestartConfig>builder()
                    .clazz(RollingRestartConfig.class)
                    .config(rollingRestartConfig)
                    .build());
            yield RollingRestart.class;
          }
          case STATUS -> Status.class;
          default -> throw new GenericApplicationException(
              ApplicationError.INVALID_OPERATION,
              this.operationName,
              Arrays.toString(Operations.values()));
        };

    modules.addAll(this.getGuiceModules());
    Operation operation = this.initializeGuiceModules(modules).getInstance(operationClass);

    if (operation.execute()) {
      Application.getState().setDeployConfig(this.deployConfig);
    }
  }

  private void shutdown() {
    this.closeClients();
    this.writeState();
  }

  @SneakyThrows
  void writeState() {
    if (Objects.nonNull(Application.getState())) {
      Application.getState().incrementVersion();
      FileUtils.writeStringToFile(
          new File(Constants.COMPONENT_STATE_FILE),
          Application.getObjectMapper().writeValueAsString(Application.getState()),
          Charset.defaultCharset());
    }
  }

  @SneakyThrows
  void readConfigFromEnvVariables() {
    this.config = System.getenv(Constants.CONFIG);
    this.componentMetadata =
        Application.getObjectMapper()
            .readValue(System.getenv(Constants.COMPONENT_METADATA), ComponentMetadata.class);
    this.componentMetadata.validate();

    this.dockerRegistryData =
        ApplicationUtil.getServiceWithCategory(
                this.componentMetadata, Constants.DOCKER_REGISTRY_SERVICE, DockerRegistryData.class)
            .stream()
            .filter(DockerRegistryData::isAllowPush)
            .reduce(
                (r1, r2) -> {
                  throw new GenericApplicationException(
                      ApplicationError.MULTIPLE_DOCKER_REGISTRY_FOR_PUBLISHING);
                })
            .orElseThrow(
                () ->
                    new GenericApplicationException(
                        ApplicationError.NO_DOCKER_REGISTRY_FOR_PUBLISHING));

    this.awsAccountData =
        Application.getObjectMapper()
            .convertValue(
                this.componentMetadata.getCloudProviderDetails().getAccount().getData(),
                AwsAccountData.class);

    this.eksData =
        ApplicationUtil.getServiceWithCategory(
            this.componentMetadata.getCloudProviderDetails().getAccount().getServices(),
            Constants.KUBERNETES,
            EKSData.class);

    this.vpcData =
        ApplicationUtil.getServiceWithCategory(
            this.componentMetadata.getCloudProviderDetails().getAccount().getServices(),
            Constants.NETWORK,
            VPCData.class);
  }

  private void initialiseClients() {
    this.dockerClient =
        new DockerClient(
            this.dockerRegistryData.getServer(),
            this.dockerRegistryData.getUsername(),
            this.dockerRegistryData.getPassword(),
            this.dockerRegistryData.getRegistry());
    this.k8sClient = new K8sClient();
    this.helmClient = new HelmClient();
  }

  private void closeClients() {
    if (this.k8sClient != null) {
      this.k8sClient.close();
    }
  }

  private Injector initializeGuiceModules(List<Module> modules) {
    return Guice.createInjector(modules);
  }

  private List<Module> getGuiceModules() {
    return new ArrayList<>(
        List.of(
            AwsModule.builder()
                .dockerClient(this.dockerClient)
                .k8sClient(this.k8sClient)
                .helmClient(this.helmClient)
                .build(),
            ConfigModule.builder()
                .componentMetadata(this.componentMetadata)
                .deployConfig(this.deployConfig)
                .awsAccountData(this.awsAccountData)
                .dockerRegistryData(this.dockerRegistryData)
                .eksData(this.eksData)
                .vpcData(this.vpcData)
                .objectMapper(Application.getObjectMapper())
                .build()));
  }
}
