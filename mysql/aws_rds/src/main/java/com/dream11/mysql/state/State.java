package com.dream11.mysql.state;

import com.dream11.mysql.config.user.DeployConfig;
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
  Map<String, String> readerInstanceIdentifiers;

  String clusterParameterGroupName;
  String writerInstanceParameterGroupName;
  Map<String, String> readerInstanceParameterGroupNames;

  String writerEndpoint;
  String readerEndpoint;

  public void incrementVersion() {
    this.version++;
  }
}
