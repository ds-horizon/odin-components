package com.dream11.mysql.constant;

import com.dream11.mysql.util.ApplicationUtil;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public final String COMPONENT_METADATA = "COMPONENT_METADATA";
  public final String COMPONENT_STATE_FILE = "state.json";
  public final String PROJECT_PROPERTIES = "project.properties";
  public final String RDS_CATEGORY = "RELATIONAL_DATABASE";
  public final String CONFIG = "CONFIG";
  public final int MAX_ATTEMPTS = 10;
  public final int RETRY_DELAY = 3;
  public final int RETRY_MAX_BACKOFF = 120;

  public final Map<String, String> COMPONENT_TAGS =
      Map.of("component:mysql:version", ApplicationUtil.getProjectVersion());
  public final String ENGINE_TYPE = "aurora-mysql";
  public final String CLUSTER_PARAMETER_GROUP_SUFFIX = "cpg";
  public final String INSTANCE_PARAMETER_GROUP_SUFFIX = "ipg";
  public final String CLUSTER_SUFFIX = "cluster";
  public final String WRITER_INSTANCE_SUFFIX = "writer";
  public final String READER_INSTANCE_SUFFIX = "reader";
}
