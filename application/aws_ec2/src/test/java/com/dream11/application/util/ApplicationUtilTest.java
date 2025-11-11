package com.dream11.application.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dream11.application.config.metadata.Account;
import com.dream11.application.config.metadata.aws.EC2Data;
import com.dream11.application.constant.Constants;
import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.error.ErrorCategory;
import com.dream11.application.exception.GenericApplicationException;
import freemarker.core.InvalidReferenceException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationUtilTest {

  @Test
  void testGetIdentifiers() {
    // Arrange
    List<String> expectedIdentifiers = List.of("1ib", "1ig", "2ib", "2ig");
    // Act
    List<String> identifiers = ApplicationUtil.getIdentifiers(2, 'i');
    // Assert
    assertThat(identifiers).containsExactlyInAnyOrderElementsOf(expectedIdentifiers);
  }

  @ParameterizedTest
  @MethodSource("privateIdentifiers")
  void testGetPrivateIdentifiers(
      Integer stacks, DiscoveryType discoveryType, List<String> expectedIdentifiers) {
    // Act
    List<String> identifiers = ApplicationUtil.getPrivateIdentifiers(stacks, discoveryType);
    // Assert
    assertThat(identifiers).containsExactlyInAnyOrderElementsOf(expectedIdentifiers);
  }

  @ParameterizedTest
  @MethodSource("publicIdentifiers")
  void testGetPublicIdentifiers(
      Integer stacks, DiscoveryType discoveryType, List<String> expectedIdentifiers) {
    // Act
    List<String> identifiers = ApplicationUtil.getPublicIdentifiers(stacks, discoveryType);
    // Assert
    assertThat(identifiers).containsExactlyInAnyOrderElementsOf(expectedIdentifiers);
  }

  @Test
  void testGenerateRandomId() {
    // Arrange
    int length = 10;

    // Act

    String randomId = ApplicationUtil.generateRandomId(length);

    // Assert

    assertThat(randomId).hasSize(length);
  }

  @Test
  void testGetServiceWithCategory() {
    // Arrange
    Map<String, Object> data = Map.of("iamInstanceProfile", "admin-role");
    Account.Service ec2Service = new Account.Service("EC2", "VM", data);
    Account.Service route53Service = new Account.Service("ROUTE53", "DISCOVERY", Map.of());
    // Act
    EC2Data ec2Data =
        ApplicationUtil.getServiceWithCategory(
            List.of(ec2Service, route53Service), "VM", EC2Data.class);
    // Assert
    assertThat(ec2Data.getIamInstanceProfile()).isEqualTo("admin-role");
    assertThat(ec2Data.getUserData()).isNull();
    assertThat(ec2Data.getTags()).isEmpty();
    assertThat(ec2Data.getAmi()).isNull();
    assertThat(ec2Data.getEc2KeyName()).isNull();
  }

  @Test
  void testGetServiceWithCategoryThrowsException() {
    // Arrange
    List<Account.Service> services = List.of(new Account.Service("S2", "DISCOVERY", Map.of()));
    // Act & Assert
    assertThatThrownBy(() -> ApplicationUtil.getServiceWithCategory(services, "VM", EC2Data.class))
        .isInstanceOf(GenericApplicationException.class)
        .hasMessage(
            String.format("%s: No service with category:[VM] found", ErrorCategory.ODIN_ERROR));
  }

  @Test
  void testMerge() {
    // Arrange
    Map<String, String> map1 = Map.of("k1", "v1", "k2", "v2");
    Map<String, String> map2 = Map.of("k2", "vv2", "k3", "v3");
    Map<String, String> expectedMergedMap = Map.of("k1", "v1", "k2", "vv2", "k3", "v3");

    // Act
    Map<String, String> mergedMap = ApplicationUtil.merge(List.of(map1, map2));

    // Assert
    assertThat(mergedMap).hasSize(3).containsExactlyInAnyOrderEntriesOf(expectedMergedMap);
  }

  @ParameterizedTest
  @MethodSource("weightDistributions")
  void testGetWeightDistribution(int stacks, Map<String, Long> expectedDistribution) {
    // Arrange & Act
    Map<String, Long> distribution = ApplicationUtil.getWeightDistribution(stacks);

    // Assert
    assertThat(distribution).containsExactlyInAnyOrderEntriesOf(expectedDistribution);
  }

  private static Stream<Arguments> privateIdentifiers() {
    List<String> identifiers = List.of("1ib", "1ig", "2ib", "2ig");
    return Stream.of(
        Arguments.of(2, DiscoveryType.PRIVATE, identifiers),
        Arguments.of(2, DiscoveryType.BOTH, identifiers),
        Arguments.of(2, DiscoveryType.PUBLIC, List.of()),
        Arguments.of(2, DiscoveryType.NONE, List.of()));
  }

  private static Stream<Arguments> publicIdentifiers() {
    List<String> identifiers = List.of("1eb", "1eg", "2eb", "2eg");
    return Stream.of(
        Arguments.of(2, DiscoveryType.PUBLIC, identifiers),
        Arguments.of(2, DiscoveryType.BOTH, identifiers),
        Arguments.of(2, DiscoveryType.PRIVATE, List.of()),
        Arguments.of(2, DiscoveryType.NONE, List.of()));
  }

  private static Stream<Arguments> weightDistributions() {
    return Stream.of(
        Arguments.of(1, Map.of("1", 100L)),
        Arguments.of(2, Map.of("1", 50L, "2", 50L)),
        Arguments.of(3, Map.of("1", 34L, "2", 33L, "3", 33L)),
        Arguments.of(4, Map.of("1", 25L, "2", 25L, "3", 25L, "4", 25L)),
        Arguments.of(
            7, Map.of("1", 16L, "2", 14L, "3", 14L, "4", 14L, "5", 14L, "6", 14L, "7", 14L)));
  }

  @Test
  void testSubstituteValues() {
    // Arrange
    String content = "Hello ${USER}!";
    Map<String, Object> dataModel = Map.of("USER", "odin");

    // Act
    String result = ApplicationUtil.substituteValues("", content, dataModel);

    // Assert
    assertThat(result).isEqualTo("Hello odin!");
  }

  @Test
  void testSubstituteValuesFailure() {
    // Arrange
    String content = "Hello ${USER}!";
    Map<String, Object> dataModel = Map.of();

    // Act & Assert
    assertThatThrownBy(() -> ApplicationUtil.substituteValues("", content, dataModel))
        .isInstanceOf(InvalidReferenceException.class);
  }

  @ParameterizedTest
  @MethodSource("deploymentStack")
  void testGetSisterDeploymentStack(Character deploymentStack, Character sisterDeploymentStack) {
    // Act
    Character alternateStack = ApplicationUtil.getSisterDeploymentStack(deploymentStack);
    // Assert
    assertThat(alternateStack).isEqualTo(sisterDeploymentStack);
  }

  @Test
  void testGetSisterDeploymentStackFailure() {
    // Act & Assert
    assertThatThrownBy(() -> ApplicationUtil.getSisterDeploymentStack('d'))
        .isInstanceOf(GenericApplicationException.class);
  }

  @ParameterizedTest
  @MethodSource("sumList")
  void testSumList(List<Double> doubles, Double sum) {
    // Act & Assert
    assertThat(ApplicationUtil.sumList(doubles)).isEqualTo(sum);
  }

  private static Stream<Arguments> deploymentStack() {
    return Stream.of(
        Arguments.of(
            Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER,
            Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER),
        Arguments.of(
            Constants.GREEN_DEPLOYMENT_STACK_IDENTIFIER,
            Constants.BLUE_DEPLOYMENT_STACK_IDENTIFIER));
  }

  private static Stream<Arguments> sumList() {
    return Stream.of(
        Arguments.of(List.of(1.0, 1.0, 100.1), 102.1),
        Arguments.of(List.of(), 0.0),
        Arguments.of(List.of(10.0, 1.0, 130.1, 42.4), 183.5));
  }
}
