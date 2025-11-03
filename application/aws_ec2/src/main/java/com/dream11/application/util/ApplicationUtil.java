package com.dream11.application.util;

import com.dream11.application.Application;
import com.dream11.application.config.metadata.Account;
import com.dream11.application.config.metadata.aws.DiscoveryData;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import freemarker.template.Template;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

@UtilityClass
@Slf4j
public class ApplicationUtil {

  public List<String> getIdentifiers(Integer stacks, Character type) {
    return IntStream.range(1, stacks + 1)
        .mapToObj(String::valueOf)
        .flatMap(
            stack ->
                Arrays.stream(
                    new String[] {
                      String.format(
                          "%s%s%s", stack, type, Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER),
                      String.format(
                          "%s%s%s", stack, type, Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER)
                    }))
        .toList();
  }

  public List<String> getPrivateIdentifiers(Integer stacks, DiscoveryType discoveryType) {
    return (discoveryType == DiscoveryType.PRIVATE || discoveryType == DiscoveryType.BOTH)
        ? ApplicationUtil.getIdentifiers(stacks, Constants.INTERNAL_IDENTIFIER)
        : List.of();
  }

  public List<String> getPublicIdentifiers(Integer stacks, DiscoveryType discoveryType) {
    return (discoveryType == DiscoveryType.PUBLIC || discoveryType == DiscoveryType.BOTH)
        ? ApplicationUtil.getIdentifiers(stacks, Constants.EXTERNAL_IDENTIFIER)
        : List.of();
  }

  public String generateRandomId(int length) {
    return RandomStringUtils.randomAlphanumeric(length);
  }

  public <T> T getServiceWithCategory(
      List<Account.Service> services, String category, Class<T> clazz) {
    Map<String, Object> data = ApplicationUtil.getServiceWithCategory(services, category).getData();
    return Application.getObjectMapper().convertValue(data, clazz);
  }

  public Account.Service getServiceWithCategory(List<Account.Service> services, String category) {
    return services.stream()
        .filter(service -> service.getCategory().equals(category))
        .findFirst()
        .orElseThrow(
            () -> new GenericApplicationException(ApplicationError.SERVICE_NOT_FOUND, category));
  }

  public Account getAccountWithCategory(List<Account> accounts, String category) {
    return accounts.stream()
        .filter(account -> account.getCategory().equals(category))
        .findFirst()
        .orElseThrow(
            () -> new GenericApplicationException(ApplicationError.ACCOUNT_NOT_FOUND, category));
  }

  /*
   Merges all the maps, last argument is of highest priority
  */
  public <K, V> Map<K, V> merge(List<Map<K, V>> maps) {
    Map<K, V> mergedMap = new HashMap<>();
    maps.forEach(mergedMap::putAll);
    return mergedMap;
  }

  @SneakyThrows
  public static String substituteValues(
      String name, String content, Map<String, Object> dataModel) {
    Template template = new Template(name, content, null);
    try (StringWriter out = new StringWriter()) {
      template.process(dataModel, out);
      return out.toString();
    }
  }

  @SneakyThrows
  public <T> List<T> runOnExecutorService(List<Callable<T>> tasks) {
    return runOnExecutorService(tasks, true);
  }

  @SneakyThrows
  public <T> List<T> runOnExecutorService(List<Callable<T>> tasks, boolean failFast) {
    List<Future<T>> results =
        tasks.stream().map(task -> Application.getExecutorService().submit(task)).toList();
    // Initialise responses
    List<T> responses = new ArrayList<>();
    for (int i = 0; i < results.size(); i++) {
      responses.add(null);
    }
    List<Throwable> exceptions = new ArrayList<>();
    Set<Integer> completedFutures = new HashSet<>();

    // Wait for futures to complete, throw exception if one of them fails.
    while (completedFutures.size() < tasks.size()) {
      for (int i = 0; i < results.size(); i++) {
        if (results.get(i).isDone() && !completedFutures.contains(i)) {
          try {
            responses.set(i, results.get(i).get());
          } catch (Exception ex) {
            if (failFast) {
              // Cancel all futures
              log.debug("Exception while executing tasks. Cancelling pending tasks.");
              results.forEach(result -> result.cancel(true));
              throw ex;
            } else {
              log.debug("Exception while executing task", ex);
              exceptions.add(ex);
            }
          }
          completedFutures.add(i);
        }
      }
    }
    if (exceptions.isEmpty()) {
      return responses;
    }
    throw new GenericApplicationException(
        ApplicationError.ODIN_ERROR,
        exceptions.stream().map(Throwable::getMessage).collect(Collectors.joining(", ")));
  }

  public <T> void validate(T object) {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<T>> constraintViolations = validator.validate(object);
    if (!constraintViolations.isEmpty()) {
      throw new GenericApplicationException(
          ApplicationError.CONSTRAINT_VIOLATION,
          new ConstraintViolationException(constraintViolations).getMessage());
    }
  }

  public Character getSisterDeploymentStack(Character deploymentStack) {
    if (deploymentStack.equals(Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER)) {
      return Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER;
    } else if (deploymentStack.equals(Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER)) {
      return Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER;
    } else {
      throw new GenericApplicationException(
          ApplicationError.INVALID_DEPLOYMENT_STACK,
          Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER,
          Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER);
    }
  }

  public String getDeploymentStackName(Character deploymentStackIdentifier) {
    return deploymentStackIdentifier.equals(Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER)
        ? Constants.BLUE_DEPLOYMENT_STACK_NAME
        : Constants.GREEN_DEPLOYMENT_STACK_NAME;
  }

  public Map<String, Long> getWeightDistribution(int stacks) {
    Map<String, Long> weightDistribution =
        IntStream.range(1, stacks + 1)
            .mapToObj(String::valueOf)
            .collect(Collectors.toMap(v -> v, v -> 100L / stacks));
    weightDistribution.put("1", 100L / stacks + 100 % stacks);
    return weightDistribution;
  }

  public Double sumList(List<Double> doubles) {
    return doubles.stream().reduce(0.0, Double::sum);
  }

  public String getCertificateArn(DiscoveryData discoveryData, String route) {
    return discoveryData.getDomainFromRoute(route).getCertificateArn();
  }

  @SneakyThrows
  public String getProjectVersion() {
    try (InputStream inputStream =
        ApplicationUtil.class.getClassLoader().getResourceAsStream(Constants.PROJECT_PROPERTIES)) {
      if (Objects.isNull(inputStream)) {
        return "null";
      }
      Properties properties = new Properties();
      properties.load(inputStream);
      return properties.getProperty("version", "null");
    }
  }

  public Throwable getRootCause(Throwable throwable) {
    return Objects.isNull(throwable.getCause()) ? throwable : getRootCause(throwable.getCause());
  }

  @SneakyThrows
  public String readTemplateFile(String filePath, Map<String, Object> dataModel) {
    try (InputStream inputStream =
        Application.class.getClassLoader().getResourceAsStream(filePath)) {
      if (Objects.isNull(inputStream)) {
        throw new GenericApplicationException(ApplicationError.TEMPLATE_FILE_NOT_FOUND, filePath);
      }
      return ApplicationUtil.substituteValues(
          "content", IOUtils.toString(inputStream, Charset.defaultCharset()), dataModel);
    }
  }
}
