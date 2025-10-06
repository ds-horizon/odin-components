package com.dream11.mysql.util;

import com.dream11.mysql.Application;
import com.dream11.mysql.config.metadata.Account;
import com.dream11.mysql.constant.Constants;
import com.dream11.mysql.error.ApplicationError;
import com.dream11.mysql.exception.GenericApplicationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import com.dream11.mysql.annotation.ParameterName;
import java.lang.reflect.Field;

@UtilityClass
@Slf4j
public class ApplicationUtil {

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

  /*
   Merges all the maps, last argument is of highest priority
  */
  public <K, V> Map<K, V> merge(List<Map<K, V>> maps) {
    Map<K, V> mergedMap = new HashMap<>();
    maps.forEach(mergedMap::putAll);
    return mergedMap;
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

  public String joinByHyphen(String... strings) {
    return String.join("-", strings);
  }

  public <T> void setIfNotNull(Consumer<T> setter, T value) {
    if (value != null) {
      setter.accept(value);
    }
  }

  @SneakyThrows
  public Map<String, Object> extractParameters(Object config) {
    Map<String, Object> parameters = new HashMap<>();
    
    if (config == null) {
      return parameters;
    }
    
    Class<?> clazz = config.getClass();
    Field[] fields = clazz.getDeclaredFields();
    
    for (Field field : fields) {
      ParameterName parameterAnnotation = field.getAnnotation(ParameterName.class);
      if (parameterAnnotation != null) {
        field.setAccessible(true);
        Object value = field.get(config);
        
        if (value != null) {
          parameters.put(parameterAnnotation.value(), value);
        }
      }
    }
    
    return parameters;
  }
}
