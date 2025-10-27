package com.dream11.application.aws;

import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

@Slf4j
public class HttpClient {

  final AwsCredentials credentials;
  final String region;
  final Aws4Signer aws4Signer;
  final SdkHttpClient sdkHttpClient;
  final Integer maxRetries;
  final Integer baseDelay;
  final Integer maxBackoff;
  final Random random;

  public HttpClient(String region, Integer maxRetries, Integer baseDelay, Integer maxBackoff) {
    try (DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create()) {
      this.credentials = credentialsProvider.resolveCredentials();
    }
    this.region = region;
    this.aws4Signer = Aws4Signer.create();
    this.sdkHttpClient = ApacheHttpClient.create();
    this.maxRetries = maxRetries;
    this.baseDelay = baseDelay;
    this.maxBackoff = maxBackoff;
    this.random = new Random();
  }

  private SdkHttpFullRequest signRequest(SdkHttpFullRequest request, String serviceName) {
    return this.aws4Signer.sign(
        request,
        Aws4SignerParams.builder()
            .awsCredentials(this.credentials)
            .signingRegion(Region.of(this.region))
            .signingName(serviceName)
            .build());
  }

  @SneakyThrows
  public HttpResponse executeRequest(SdkHttpFullRequest request, String serviceName) {
    SdkHttpFullRequest signedRequest = this.signRequest(request, serviceName);
    HttpExecuteRequest executeRequest = HttpExecuteRequest.builder().request(signedRequest).build();
    HttpResponse result;

    int attempt = 0;
    while (attempt < this.maxRetries) {
      int delay = calculateEqualJitterBackoff(attempt);

      try {
        result = getResponse(this.sdkHttpClient.prepareRequest(executeRequest).call());

        if (result.isSuccessful()) {
          return result;
        } else if (isThrottlingError(result)) {
          log.info(
              "Http request failed due to throttling. Waiting for [{}] seconds before retry...",
              delay);
        } else if (isLbNotProvisionedError(result)) {
          log.info(
              "Load Balancer is not yet fully provisioned. Waiting for [{}] seconds before retry...",
              delay);
        } else if (isSignatureExpired(result)) {
          log.info(
              "Http request signature expired. Waiting for [{}] seconds before retry...", delay);
          signedRequest = this.signRequest(request, serviceName);
          executeRequest = HttpExecuteRequest.builder().request(signedRequest).build();
        } else {
          return result;
        }
      } catch (Exception e) {
        log.warn(
            String.format("Http request failed, Waiting for [%s] seconds before retry...", delay),
            e);
      }

      attempt++;
      Thread.sleep(delay * 1000L);
    }
    return getResponse(this.sdkHttpClient.prepareRequest(executeRequest).call());
  }

  @SneakyThrows
  private HttpResponse getResponse(HttpExecuteResponse result) {
    try (AbortableInputStream responseBodyStream = result.responseBody().orElseThrow()) {
      return HttpResponse.builder()
          .statusCode(result.httpResponse().statusCode())
          .statusText(result.httpResponse().statusText().toString())
          .response(new String(responseBodyStream.readAllBytes()))
          .build();
    }
  }

  private int calculateEqualJitterBackoff(int attempt) {
    int ceil = Math.min(this.baseDelay * (1 << attempt), this.maxBackoff);
    return ceil / 2 + random.nextInt(ceil / 2 + 1);
  }

  private boolean isThrottlingError(HttpResponse result) {
    return result.getResponse().contains("Throttling")
        && result.getResponse().contains("Rate exceeded");
  }

  private boolean isLbNotProvisionedError(HttpResponse result) {
    return result.getResponse().contains("Load Balancer has not been fully provisioned");
  }

  private boolean isSignatureExpired(HttpResponse result) {
    return result.getResponse().contains("SignatureDoesNotMatch")
        && result.getResponse().contains("Signature expired");
  }

  @Data
  @Builder
  @AllArgsConstructor
  public static class HttpResponse {
    int statusCode;
    String statusText;
    String response;

    public boolean isSuccessful() {
      return this.statusCode >= 200 && this.statusCode < 300;
    }
  }
}
