package com.dream11.application.util;

import com.dream11.application.Application;
import com.dream11.application.config.ComponentMetadata;
import com.dream11.application.config.metadata.Account;
import com.dream11.application.constant.Constants;
import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import freemarker.template.Template;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
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

  public <T> List<T> getServiceWithCategory(
      ComponentMetadata componentMetadata, String category, Class<T> clazz) {
    return ApplicationUtil.getServiceWithCategory(componentMetadata, category).stream()
        .map(service -> Application.getObjectMapper().convertValue(service.getData(), clazz))
        .toList();
  }

  public List<Account.Service> getServiceWithCategory(
      ComponentMetadata componentMetadata, String category) {
    return Stream.concat(
            componentMetadata.getCloudProviderDetails().getAccount().getServices().stream(),
            componentMetadata.getCloudProviderDetails().getLinkedAccounts().stream()
                .flatMap(account -> account.getServices().stream()))
        .filter(service -> service.getCategory().equals(category))
        .toList();
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

  public static String parseCpu(String cpu) {
    double value;
    if (cpu.endsWith("m")) {
      value = Double.parseDouble(cpu.substring(0, cpu.length() - 1));
    } else {
      value = Double.parseDouble(cpu) * 1000.0;
    }
    return Integer.toString((int) Math.floor(value));
  }

  public static String parseMemory(String memory) {
    double valueInBytes;
    if (memory.endsWith("Ki")) {
      valueInBytes = Double.parseDouble(memory.replace("Ki", "")) * 1024.0;
    } else if (memory.endsWith("Mi")) {
      valueInBytes = Double.parseDouble(memory.replace("Mi", "")) * (1024.0 * 1024.0);
    } else if (memory.endsWith("Gi")) {
      valueInBytes = Double.parseDouble(memory.replace("Gi", "")) * (1024.0 * 1024.0 * 1024.0);
    } else if (memory.endsWith("Ti")) {
      valueInBytes =
          Double.parseDouble(memory.replace("Ti", "")) * (1024.0 * 1024.0 * 1024.0 * 1024.0);
    } else if (memory.endsWith("K")) {
      valueInBytes = Double.parseDouble(memory.replace("K", "")) * 1000.0;
    } else if (memory.endsWith("M")) {
      valueInBytes = Double.parseDouble(memory.replace("M", "")) * (1000.0 * 1000.0);
    } else if (memory.endsWith("G")) {
      valueInBytes = Double.parseDouble(memory.replace("G", "")) * (1000.0 * 1000.0 * 1000.0);
    } else if (memory.endsWith("T")) {
      valueInBytes =
          Double.parseDouble(memory.replace("T", "")) * (1000.0 * 1000.0 * 1000.0 * 1000.0);
    } else {
      throw new IllegalArgumentException("Unknown memory format: " + memory);
    }
    return Integer.toString((int) Math.floor(valueInBytes / (1024.0 * 1024.0)));
  }
}
