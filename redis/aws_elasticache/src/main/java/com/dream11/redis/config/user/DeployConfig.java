package com.dream11.redis.config.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dream11.redis.Application;
import com.dream11.redis.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class DeployConfig implements Config {
  @NotNull
  private String redisVersion;

  @Size(max = 255)
  private String replicationGroupDescription = "ElastiCache Redis replication group";

  @NotNull
  @Pattern(regexp = "^cache\\.[a-z0-9]+\\.(micro|small|medium|large|xlarge|[0-9]+xlarge)$")
  private String cacheNodeType = "cache.t4g.micro";

  @Min(1)
  @Max(500)
  private Integer numNodeGroups = 1;

  @NotNull
  private Boolean clusterModeEnabled = false;

  @NotNull
  @Valid
  private AuthenticationConfig authentication;

  @Min(0)
  @Max(5)
  private Integer replicasPerNodeGroup = 0;

  private String cacheSubnetGroupName;

  @Size(min = 1)
  private List<@Pattern(regexp = "^sg-[a-f0-9]+$") String> securityGroupIds;

  private String cacheParameterGroupName;

  private Boolean automaticFailoverEnabled = false;

  private Boolean multiAzEnabled = false;

  private Boolean transitEncryptionEnabled = false;

  private Boolean atRestEncryptionEnabled = false;

  @Min(0)
  @Max(35)
  private Integer snapshotRetentionLimit = 0;

  @Pattern(regexp = "^(mon|tue|wed|thu|fri|sat|sun):[0-9]{2}:[0-9]{2}-(mon|tue|wed|thu|fri|sat|sun):[0-9]{2}:[0-9]{2}$")
  private String preferredMaintenanceWindow;

  private Map<String, String> tags = new HashMap<>();

  @Pattern(regexp = "^[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}$")
  private String snapshotWindow;

  @Pattern(regexp = "^arn:aws:sns:[a-z0-9\\-]+:[0-9]+:[a-zA-Z0-9\\-_]+$")
  private String notificationTopicArn;

  private Boolean autoMinorVersionUpgrade = true;

  private List<@Pattern(regexp = "^[a-z]{2}-[a-z]+-[0-9][a-z]$") String> preferredCacheClusterAZs;

  private String kmsKeyId;

  @Override
  public void validate() {
    Config.super.validate();
  }

  @SneakyThrows
  public DeployConfig mergeWith(String overrides) {
    ObjectMapper objectMapper = Application.getObjectMapper();
    JsonNode node = objectMapper.readValue(objectMapper.writeValueAsString(this), JsonNode.class);
    return objectMapper.readValue(
        objectMapper.readerForUpdating(node).readValue(overrides).toString(), DeployConfig.class);
  }
}
