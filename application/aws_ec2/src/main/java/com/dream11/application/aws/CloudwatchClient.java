package com.dream11.application.aws;

import com.dream11.application.entity.CloudWatchMetric;
import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;

public class CloudwatchClient {
  final CloudWatchClient client;

  public CloudwatchClient(String region, RetryStrategy retryStrategy) {
    this.client =
        CloudWatchClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryStrategy(retryStrategy))
            .build();
  }

  public List<MetricDataResult> getMetric(
      Instant startTime, Instant endTime, CloudWatchMetric metric) {
    GetMetricDataRequest request =
        GetMetricDataRequest.builder()
            .startTime(startTime)
            .endTime(endTime)
            .metricDataQueries(
                MetricDataQuery.builder()
                    .id("id")
                    .metricStat(
                        statBuilder ->
                            statBuilder
                                .stat(metric.getStatistic())
                                .period(60)
                                .metric(
                                    metricBuilder ->
                                        metricBuilder
                                            .namespace(metric.getNamespace())
                                            .metricName(metric.getMetricName())
                                            .dimensions(
                                                Dimension.builder()
                                                    .name(metric.getResourceName())
                                                    .value(metric.getResourceValue())
                                                    .build())))
                    .build())
            .build();
    return this.client.getMetricData(request).metricDataResults();
  }
}
