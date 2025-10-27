package com.dream11.application.util;

import com.dream11.application.Application;
import com.dream11.application.config.metadata.Account;
import com.dream11.application.config.metadata.ComponentMetadata;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import freemarker.template.Template;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
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

  public <T> List<T> getServicesWithCategory(
      ComponentMetadata componentMetadata, String category, Class<T> clazz) {
    return ApplicationUtil.getServicesWithCategory(componentMetadata, category).stream()
        .map(service -> Application.getObjectMapper().convertValue(service.getData(), clazz))
        .toList();
  }

  public List<Account.Service> getServicesWithCategory(
      ComponentMetadata componentMetadata, String category) {
    return Stream.concat(
            componentMetadata.getCloudProviderDetails().getAccount().getServices().stream(),
            componentMetadata.getCloudProviderDetails().getLinkedAccounts().stream()
                .flatMap(account -> account.getServices().stream()))
        .filter(service -> service.getCategory().equals(category))
        .toList();
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

  public Throwable getRootCause(Throwable throwable) {
    return Objects.isNull(throwable.getCause()) ? throwable : getRootCause(throwable.getCause());
  }
}
