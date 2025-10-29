package com.dream11.mysql.state;

import com.dream11.mysql.config.user.DeployConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class State {
  long version;
  DeployConfig deployConfig;

  String clusterIdentifier;
  String writerInstanceIdentifier;
  @Builder.Default Map<String, List<String>> readerInstanceIdentifiers = new HashMap<>();

  String clusterParameterGroupName;
  String instanceParameterGroupName;

  String writerEndpoint;
  String readerEndpoint;

  public void incrementVersion() {
    this.version++;
  }
}
