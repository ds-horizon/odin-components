package com.dream11.application;

import com.dream11.application.client.DockerClient;
import com.dream11.application.client.HelmClient;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.config.metadata.local.DockerRegistryData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.Operations;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.error.ErrorCategory;
import com.dream11.application.exception.GenericApplicationException;
import com.dream11.application.inject.ClientModule;
import com.dream11.application.inject.ConfigModule;
import com.dream11.application.operation.Deploy;
import com.dream11.application.operation.ImageTemplate;
import com.dream11.application.operation.Operation;
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
  DeployConfig deployConfig;

  HelmClient helmClient;

  DockerClient dockerClient;

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
      } else {
        log.error("{}: {}", ErrorCategory.ODIN_ERROR, rootCause.getMessage());
      }
      log.debug("Error:", rootCause); // Logging stack trace in debug to avoid it being sent to CLI
      System.exit(1);
    }
  }

  void start() {
    this.addShutdownHook();
    this.readState();
    this.readConfigFromEnvVariables();
    this.initialiseClients();
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
          case UNDEPLOY -> {
            this.deployConfig = null;
            yield Undeploy.class;
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
          case REDEPLOY -> {
            if (Objects.nonNull(this.deployConfig)) {
              this.deployConfig = this.deployConfig.mergeWith(this.config);
            } else {
              throw new GenericApplicationException(
                  ApplicationError.DEPLOY_CONFIG_NOT_FOUND_IN_STATE, "redeploy");
            }
            yield Deploy.class;
          }
        };

    Operation operation =
        this.initializeGuiceModules(this.getGuiceModules()).getInstance(operationClass);
    log.debug("Executing operation:[{}]", Operations.fromValue(this.operationName));
    if (Operations.fromValue(this.operationName) == Operations.UNDEPLOY) {
      log.info("Deleting all created resources");
    } else {
      log.debug("Desired deployment config: {}", this.deployConfig);
    }
    if (operation.execute()) {
      Application.getState()
          .setDeployConfig(
              this.deployConfig); // Update deployment config in state after successful execution
    }
  }

  private void shutdown() {
    this.writeState(); // Write state to file in all cases
  }

  @SneakyThrows
  private void writeState() {
    Application.getState().incrementVersion();
    log.debug("Final state: {}", Application.getState());
    log.debug("Writing state");
    FileUtils.writeStringToFile(
        new File(Constants.COMPONENT_STATE_FILE),
        Application.getObjectMapper().writeValueAsString(Application.getState()),
        Charset.defaultCharset());
  }

  @SneakyThrows
  private void readConfigFromEnvVariables() {
    this.config = System.getenv(Constants.CONFIG);
    this.componentMetadata =
        Application.getObjectMapper()
            .readValue(System.getenv(Constants.COMPONENT_METADATA), ComponentMetadata.class);
    this.componentMetadata.validate();

    this.dockerRegistryData =
        ApplicationUtil.getServicesWithCategory(
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
  }

  private void initialiseClients() {
    String cloudProvider =
        this.componentMetadata.getCloudProviderDetails().getAccount().getProvider();
    if (!cloudProvider.equalsIgnoreCase(Constants.LOCAL)) {
      throw new GenericApplicationException(ApplicationError.INVALID_CLOUD_PROVIDER, cloudProvider);
    }
    this.helmClient = new HelmClient();
    this.dockerClient =
        new DockerClient(
            this.dockerRegistryData.getServer(),
            this.dockerRegistryData.getUsername(),
            this.dockerRegistryData.getPassword(),
            this.dockerRegistryData.getRegistry());
  }

  private Injector initializeGuiceModules(List<Module> modules) {
    return Guice.createInjector(modules);
  }

  private List<Module> getGuiceModules() {
    return List.of(
        ClientModule.builder().helmClient(this.helmClient).dockerClient(this.dockerClient).build(),
        ConfigModule.builder()
            .componentMetadata(this.componentMetadata)
            .deployConfig(this.deployConfig)
            .dockerRegistryData(this.dockerRegistryData)
            .build());
  }
}
