package com.dream11.application.config.aws;

import lombok.Data;

@Data
public class DockerRegistryData {
  private String server;
  private String registry;
  private String username;
  private String password;
  private boolean allowPush;
  private boolean insecure;
}
