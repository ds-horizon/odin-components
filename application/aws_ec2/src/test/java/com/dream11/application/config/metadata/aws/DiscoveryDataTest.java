package com.dream11.application.config.metadata.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.application.error.ErrorCategory;
import com.dream11.application.exception.GenericApplicationException;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiscoveryDataTest {
  static DiscoveryData discoveryData;

  @BeforeAll
  @SneakyThrows
  static void setup() {
    discoveryData =
        DiscoveryData.builder()
            .domains(
                Arrays.asList(
                    DiscoveryData.Domain.builder()
                        .id("privateId")
                        .name("dream11-load.local")
                        .isActive(true)
                        .build(),
                    DiscoveryData.Domain.builder()
                        .id("publicId")
                        .name("d11load.com")
                        .isActive(false)
                        .build()))
            .build();
  }

  @Test
  void testGetDomainSuccess() {
    DiscoveryData.Domain domain = discoveryData.getDomainFromRoute("test-route.dream11-load.local");
    assertThat(domain.getId()).isEqualTo("privateId");
  }

  @Test
  void testGetDomainFailIfDomainNotFound() {
    String route = "test-route.nonexistent.com";
    assertThatThrownBy(() -> discoveryData.getDomainFromRoute(route))
        .isInstanceOf(GenericApplicationException.class)
        .hasMessage(
            String.format("%s: No domain found for route:[%s]", ErrorCategory.ODIN_ERROR, route));
  }
}
