package com.dream11.mysql;

import com.dream11.mysql.client.RDSClient;
import com.dream11.mysql.config.metadata.ComponentMetadata;
import com.dream11.mysql.config.metadata.aws.AwsAccountData;
import com.dream11.mysql.config.user.DeployConfig;
import com.dream11.mysql.constant.Constants;
import com.dream11.mysql.constant.Operations;
import com.dream11.mysql.error.ApplicationError;
import com.dream11.mysql.error.ErrorCategory;
import com.dream11.mysql.exception.GenericApplicationException;
import com.dream11.mysql.inject.AwsModule;
import com.dream11.mysql.inject.ConfigModule;
import com.dream11.mysql.operation.*;
import com.dream11.mysql.operation.Operation;
import com.dream11.mysql.operation.Redeploy;
import com.dream11.mysql.operation.Undeploy;
import com.dream11.mysql.state.State;
import com.dream11.mysql.util.ApplicationUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
  static final XmlMapper xmlMapper =
      XmlMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .withConfigOverride(
              ArrayNode.class, mutableConfigOverride -> mutableConfigOverride.setMergeable(false))
          .build();

  @Getter
  static final ExecutorService executorService =
      new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0L, TimeUnit.SECONDS, new SynchronousQueue<>());

  /* Add Cloud specific clients */

  /* Common configs */
  ComponentMetadata componentMetadata;
  String config;
  final String operationName;

  DeployConfig deployConfig;
  AwsAccountData awsAccountData;
  RDSClient rdsClient;

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
    this.initializeCloudProviderClients();
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
    operationClass =
        switch (Operations.fromValue(this.operationName)) {
          case DEPLOY -> {
            this.deployConfig =
                Application.getObjectMapper().readValue(this.config, DeployConfig.class);
            yield Deploy.class;
          }
          case UNDEPLOY -> Undeploy.class;
          case REDEPLOY -> Redeploy.class;
            /* Add operation */
        };

    List<Module> modules = new ArrayList<>(this.getGuiceModules());
    Operation operation = this.initializeGuiceModules(modules).getInstance(operationClass);
    log.debug("Executing operation:[{}]", Operations.fromValue(this.operationName));
    if (Operations.fromValue(this.operationName).equals(Operations.UNDEPLOY)) {
      log.info("Deleting all created resources");
    } else {
      log.info("Executing operation:[{}]", Operations.fromValue(this.operationName));
    }
    if (operation.execute()) {
      Application.getState().setDeployConfig(this.deployConfig);
      log.info("Executed operation:[{}]", Operations.fromValue(this.operationName));
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
    JsonNode node =
        Application.getObjectMapper().readTree(System.getenv(Constants.COMPONENT_METADATA));
    this.componentMetadata =
        Application.getObjectMapper().treeToValue(node, ComponentMetadata.class);
    this.componentMetadata.validate();
    this.awsAccountData =
        Application.getObjectMapper()
            .convertValue(
                this.componentMetadata.getCloudProviderDetails().getAccount().getData(),
                AwsAccountData.class);
    this.awsAccountData.validate();

    this.config = System.getenv(Constants.CONFIG);

    /* Cloud specific configs */
  }

  void initializeCloudProviderClients() {
    String region = this.awsAccountData.getRegion();
    this.rdsClient = new RDSClient(region);
  }

  private Injector initializeGuiceModules(List<Module> modules) {
    return Guice.createInjector(modules);
  }

  private List<Module> getGuiceModules() {
    List<Module> modules = new ArrayList<>();
    modules.add(
        ConfigModule.builder()
            .componentMetadata(this.componentMetadata)
            .deployConfig(this.deployConfig)
            .build());

    modules.add(AwsModule.builder().rdsClient(this.rdsClient).build());
    return modules;
  }
}
