package com.dream11.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dream11.application.aws.Route53Client;
import com.dream11.application.config.metadata.aws.DiscoveryData;
import com.dream11.application.config.user.DeployConfig;
import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.entity.Route53Record;
import com.dream11.application.util.TestUtil;
import java.util.Arrays;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class Route53ServiceTest {

  @Mock Route53Client route53Client;

  static DeployConfig deployConfig;

  static DiscoveryData discoveryData;

  @BeforeAll
  @SneakyThrows
  static void setup() {
    deployConfig = TestUtil.buildDeployConfig(1, DiscoveryType.BOTH);
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
                        .isActive(true)
                        .build()))
            .build();
  }

  @Test
  void testGetExpandedRoute53Weights() {
    // Arrange
    Route53Service route53Service = mock(Route53Service.class);
    Map<String, Long> weights =
        Map.of(
            "0ib", 100L,
            "0ig", 0L,
            "0eb", 50L,
            "0eg", 50L,
            "1ib", 0L,
            "1ig", 100L);

    Map<String, Map<Character, Map<Character, Long>>> expectedExpandedWeightMap =
        Map.of(
            "0",
                Map.of(
                    'i',
                        Map.of(
                            'b', 100L,
                            'g', 0L),
                    'e',
                        Map.of(
                            'b', 50L,
                            'g', 50L)),
            "1",
                Map.of(
                    'i',
                    Map.of(
                        'b', 0L,
                        'g', 100L)));
    when(route53Service.getRoute53Weights()).thenReturn(weights);
    when(route53Service.getExpandedRoute53Weights()).thenCallRealMethod();

    // Act
    Map<String, Map<Character, Map<Character, Long>>> expandedWeights =
        route53Service.getExpandedRoute53Weights();

    // Assert
    assertThat(expandedWeights).containsExactlyInAnyOrderEntriesOf(expectedExpandedWeightMap);
  }

  @Test
  void testGetRoute53Weights() {
    // Arrange
    Route53Service route53Service =
        new Route53Service(this.route53Client, discoveryData, deployConfig);

    String route = deployConfig.getDiscoveryConfig().getPrivateRoute();
    when(this.route53Client.get(discoveryData.getDomainFromRoute(route).getId(), route, "1ib"))
        .thenReturn(Route53Record.builder().weight(100L).build());
    when(this.route53Client.get(discoveryData.getDomainFromRoute(route).getId(), route, "1ig"))
        .thenReturn(Route53Record.builder().weight(0L).build());
    route = deployConfig.getDiscoveryConfig().getPublicRoute();
    when(this.route53Client.get(discoveryData.getDomainFromRoute(route).getId(), route, "1eb"))
        .thenReturn(Route53Record.builder().weight(20L).build());
    when(this.route53Client.get(discoveryData.getDomainFromRoute(route).getId(), route, "1eg"))
        .thenReturn(Route53Record.builder().weight(80L).build());

    // Act
    Map<String, Long> weights = route53Service.getRoute53Weights();

    // Assert
    assertThat(weights)
        .containsExactlyInAnyOrderEntriesOf(Map.of("1ib", 100L, "1ig", 0L, "1eb", 20L, "1eg", 80L));
  }
}
