package com.dream11.mysql.constant;

import java.time.Duration;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public final String BASE_CONFIG = "BASE_CONFIG";
  public final String FLAVOUR_CONFIG = "FLAVOUR_CONFIG";
  public final String OPERATION_CONFIG = "OPERATION_CONFIG";
  public final String COMPONENT_METADATA = "COMPONENT_METADATA";
  public final String COMPONENT_STATE_FILE = "state.json";
  public final String PROJECT_PROPERTIES = "project.properties";
  public final String RDS_CATEGORY = "RELATIONAL_DATABASE";
  public final Integer MAX_RETRIES = 10;
  public final Integer RETRY_DELAY = 3;
  public final Duration AWS_API_READ_TIMEOUT = Duration.ofSeconds(120);
  public final Integer RETRY_MAX_BACKOFF = 120;

  public final String CONFIG = "CONFIG";

  /* Cloud specific constants */

}
