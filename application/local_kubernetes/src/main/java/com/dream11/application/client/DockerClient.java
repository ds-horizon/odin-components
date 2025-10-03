package com.dream11.application.client;

import com.dream11.application.error.ApplicationError;
import com.dream11.application.exception.GenericApplicationException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerClient {
  final DefaultDockerClientConfig dockerClientConfig;
  final String registry;
  final HttpClient httpClient;

  public DockerClient(String url, String username, String password, String registry) {
    this.dockerClientConfig =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withRegistryUsername(username)
            .withRegistryPassword(password)
            .withRegistryUrl(url)
            .build();
    this.registry = registry;
    this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  @SneakyThrows
  public boolean checkIfImageExists(String imageName, String tag) {
    String manifestUrl =
        this.dockerClientConfig.getRegistryUrl()
            + "/v2/"
            + this.registry.split("/")[1]
            + "/"
            + imageName
            + "/manifests/"
            + tag;
    String auth =
        this.dockerClientConfig.getRegistryUsername()
            + ":"
            + this.dockerClientConfig.getRegistryPassword();
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(manifestUrl))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Accept", "application/vnd.docker.distribution.manifest.list.v2+json")
            .GET()
            .build();
    HttpResponse<String> response =
        this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      return true;
    } else if (response.statusCode() == 404) {
      return false;
    }
    throw new GenericApplicationException(
        ApplicationError.DOCKER_IMAGE_FETCH_FAILED, response.statusCode());
  }
}
