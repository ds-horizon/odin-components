package com.dream11.application.aws;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class EC2Client {

  final Ec2Client client;

  public EC2Client(String region, RetryStrategy retryStrategy, SdkHttpClient httpClient) {
    this.client =
        Ec2Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .httpClient(httpClient)
            .overrideConfiguration(overrideConfig -> overrideConfig.retryStrategy(retryStrategy))
            .build();
  }

  public Optional<String> getAMI(String name) {
    return this.client
        .describeImages(
            request ->
                request.filters(
                    Filter.builder().name("name").values(name).build(),
                    Filter.builder().name("state").values("available").build()))
        .images()
        .stream()
        .sorted(
            (i1, i2) ->
                i2.creationDate()
                    .compareTo(i1.creationDate())) // Sort in decreasing order of creation date
        .map(Image::imageId)
        .findFirst();
  }
}
