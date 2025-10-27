package com.dream11.application.aws;

import com.dream11.application.entity.Route53Record;
import com.dream11.application.exception.Route53NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.GetChangeRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

public class Route53Client {

  final software.amazon.awssdk.services.route53.Route53Client r53Client;

  public Route53Client(String region, RetryPolicy retryPolicy) {
    this.r53Client =
        software.amazon.awssdk.services.route53.Route53Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryPolicy(retryPolicy))
            .build();
  }

  private String executeUpdate(
      String hostedZoneId, List<Route53Record> records, ChangeAction changeAction) {
    return this.r53Client
        .changeResourceRecordSets(
            builder ->
                builder
                    .hostedZoneId(hostedZoneId)
                    .changeBatch(
                        changeBatchBuilder ->
                            changeBatchBuilder.changes(
                                records.stream()
                                    .map(
                                        r53record ->
                                            Change.builder()
                                                .action(changeAction)
                                                .resourceRecordSet(
                                                    ResourceRecordSet.builder()
                                                        .ttl(r53record.getTtl())
                                                        .type(RRType.CNAME)
                                                        .setIdentifier(r53record.getIdentifier())
                                                        .name(r53record.getName())
                                                        .weight(r53record.getWeight())
                                                        .resourceRecords(
                                                            resourceRecordBuilder ->
                                                                resourceRecordBuilder.value(
                                                                    r53record.getDns()))
                                                        .build())
                                                .build())
                                    .toList())))
        .changeInfo()
        .id();
  }

  public Route53Record get(String hostedZoneId, String name, String identifier) {
    ListResourceRecordSetsResponse response =
        this.r53Client.listResourceRecordSets(
            listResourceRecordSetsBuilder ->
                listResourceRecordSetsBuilder
                    .hostedZoneId(hostedZoneId)
                    .startRecordName(name)
                    .startRecordIdentifier(identifier)
                    .startRecordType(RRType.CNAME)
                    .maxItems("1"));

    if (response.hasResourceRecordSets()
        && !response.resourceRecordSets().isEmpty()
        && response.resourceRecordSets().get(0).name().equals(name + ".")
        && response.resourceRecordSets().get(0).setIdentifier().equals(identifier)) {
      ResourceRecordSet resourceRecordSet = response.resourceRecordSets().get(0);
      return Route53Record.builder()
          .name(resourceRecordSet.name())
          .weight(resourceRecordSet.weight())
          .ttl(resourceRecordSet.ttl())
          .identifier(identifier)
          .dns(resourceRecordSet.resourceRecords().get(0).value())
          .build();
    } else {
      throw new Route53NotFoundException(
          String.format("Route53 record:[%s] with identifier:[%s] not found", name, identifier));
    }
  }

  public String createOrUpdate(String hostedZoneId, List<Route53Record> records) {
    return this.executeUpdate(hostedZoneId, records, ChangeAction.UPSERT);
  }

  public void delete(String hostedZoneId, String name, String identifier) {
    this.delete(hostedZoneId, name, List.of(identifier));
  }

  public void delete(String hostedZoneId, String name, List<String> identifiers) {
    List<Route53Record> records = new ArrayList<>();
    identifiers.forEach(
        identifier -> {
          try {
            records.add(this.get(hostedZoneId, name, identifier));
          } catch (Route53NotFoundException ignored) {
            // Route 53 does not exist skip delete
          }
        });
    if (!records.isEmpty()) {
      this.executeUpdate(hostedZoneId, records, ChangeAction.DELETE);
    }
  }

  public String updateWeights(String hostedZoneId, String name, Map<String, Long> weights) {
    List<Route53Record> records =
        weights.entrySet().stream()
            .map(entry -> this.get(hostedZoneId, name, entry.getKey()).setWeight(entry.getValue()))
            .toList();

    return this.createOrUpdate(hostedZoneId, records);
  }

  public String getChange(String changeId) {
    return this.r53Client
        .getChange(GetChangeRequest.builder().id(changeId).build())
        .changeInfo()
        .statusAsString();
  }
}
