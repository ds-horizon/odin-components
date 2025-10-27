package com.dream11.application.config.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.dream11.application.constant.DiscoveryType;
import com.dream11.application.util.TestUtil;
import org.junit.jupiter.api.Test;

class DeployConfigTest {

  @Test
  void testDeepCopy() {
    // Arrange
    DeployConfig deployConfig = new DeployConfig();
    deployConfig.setStacks(2);

    // Act
    DeployConfig copy = deployConfig.deepCopy();

    // Assert
    assertThat(copy).isEqualTo(deployConfig).isNotSameAs(deployConfig);
    assertThat(copy.getLoadBalancerConfig())
        .isEqualTo(deployConfig.getLoadBalancerConfig())
        .isNotSameAs(deployConfig.getLoadBalancerConfig());
    assertThat(copy.getExtraEnvVars())
        .isEqualTo(deployConfig.getExtraEnvVars())
        .isNotSameAs(deployConfig.getExtraEnvVars());
    assertThat(copy.getAutoScalingGroupConfig())
        .isEqualTo(deployConfig.getAutoScalingGroupConfig())
        .isNotSameAs(deployConfig.getAutoScalingGroupConfig());
    assertThat(copy.getTags())
        .isEqualTo(deployConfig.getTags())
        .isNotSameAs(deployConfig.getTags());
  }

  @Test
  void testMergeWith() {
    // Arrange
    DeployConfig deployConfig = TestUtil.buildDeployConfig(2, DiscoveryType.PRIVATE);
    // Act
    DeployConfig merged =
        deployConfig.mergeWith("{\"artifact\":{\"version\":\"1.1.8\"},\"stacks\":1}");

    // Assert
    assertThat(merged.getStacks()).isEqualTo(1);
    assertThat(merged.getArtifactConfig().getName()).isEqualTo("odindemo");
    assertThat(merged.getArtifactConfig().getVersion()).isEqualTo("1.1.8");
  }
}
