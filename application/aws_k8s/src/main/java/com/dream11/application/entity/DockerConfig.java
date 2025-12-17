package com.dream11.application.entity;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DockerConfig {

  Map<String, Auth> auths;

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Data
  public static class Auth {
    String username;
    String password;
  }
}
