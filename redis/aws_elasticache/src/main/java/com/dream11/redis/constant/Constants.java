package com.dream11.redis.constant;

import com.dream11.redis.util.ApplicationUtil;
import java.time.Duration;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public final String COMPONENT_METADATA = "COMPONENT_METADATA";
  public final String COMPONENT_STATE_FILE = "state.json";
  public final String PROJECT_PROPERTIES = "project.properties";
  public final String ELASTICACHE_CATEGORY = "CACHE";
  public final String CONFIG = "CONFIG";

  public final Map<String, String> COMPONENT_TAGS =
      Map.of("component:redis:version", ApplicationUtil.getProjectVersion());
  public final String ENGINE_TYPE = "redis";

  public final String DEFAULT = "default";
  public final String PARAMETER_GROUP_SUFFIX = ".cluster.on";
  public final Duration REPLICATION_GROUP_WAIT_RETRY_TIMEOUT = Duration.ofMinutes(20);
  public final Duration REPLICATION_GROUP_WAIT_RETRY_INTERVAL = Duration.ofMillis(5000);
}
