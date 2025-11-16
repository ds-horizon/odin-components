package com.dream11.redis.util;

import com.dream11.redis.Application;
import com.dream11.redis.config.metadata.Account;
import com.dream11.redis.constant.Constants;
import com.dream11.redis.error.ApplicationError;
import com.dream11.redis.exception.GenericApplicationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

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

  public <T> void setIfNotNull(Consumer<T> setter, T value) {
    if (value != null) {
      setter.accept(value);
    }
  }
}
