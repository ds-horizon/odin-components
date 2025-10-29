package com.dream11.application.aws;

import com.dream11.application.config.user.EbsConfig;
import com.dream11.application.exception.LaunchTemplateNotFoundException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMappingRequest;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateInstanceMetadataEndpointState;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateInstanceMetadataTagsState;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateTagSpecificationRequest;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@RequiredArgsConstructor
public class LaunchTemplateClient {

  final Ec2Client ec2Client;

  public LaunchTemplateClient(String region, RetryStrategy retryStrategy) {
    this.ec2Client =
        Ec2Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .overrideConfiguration(overrideConfig -> overrideConfig.retryStrategy(retryStrategy))
            .build();
  }

  private List<LaunchTemplateBlockDeviceMappingRequest> buildBlockDeviceMappings(
      String amiId, EbsConfig ebsConfig) {
    return this.ec2Client
        .describeImages(request -> request.imageIds(amiId))
        .images()
        .get(0)
        .blockDeviceMappings()
        .stream()
        .map(
            blockDeviceMapping ->
                LaunchTemplateBlockDeviceMappingRequest.builder()
                    .deviceName(blockDeviceMapping.deviceName())
                    .ebs(
                        builder ->
                            builder
                                .snapshotId(blockDeviceMapping.ebs().snapshotId())
                                .iops(blockDeviceMapping.ebs().iops())
                                .deleteOnTermination(blockDeviceMapping.ebs().deleteOnTermination())
                                .volumeSize(ebsConfig.getSize())
                                .volumeType(blockDeviceMapping.ebs().volumeType())
                                .throughput(blockDeviceMapping.ebs().throughput())
                                .encrypted(blockDeviceMapping.ebs().encrypted()))
                    .build())
        .toList();
  }

  public LaunchTemplate create(
      String name,
      String amiId,
      String instanceType,
      String keyPair,
      String iamInstanceProfile,
      List<String> securityGroups,
      String userdata,
      EbsConfig ebsConfig,
      String imdsV2,
      Map<String, String> tags) {

    List<Tag> ec2Tags = this.getEc2Tags(tags);
    CreateLaunchTemplateRequest createLaunchTemplateRequest =
        CreateLaunchTemplateRequest.builder()
            .launchTemplateName(name)
            .launchTemplateData(
                builder ->
                    builder
                        .iamInstanceProfile(
                            iamInstanceProfileBuilder ->
                                iamInstanceProfileBuilder.name(iamInstanceProfile).build())
                        .keyName(keyPair)
                        .imageId(amiId)
                        .instanceType(instanceType)
                        .securityGroupIds(securityGroups)
                        .tagSpecifications(
                            List.of(
                                LaunchTemplateTagSpecificationRequest.builder()
                                    .resourceType(ResourceType.INSTANCE)
                                    .tags(ec2Tags)
                                    .build(),
                                LaunchTemplateTagSpecificationRequest.builder()
                                    .resourceType(ResourceType.VOLUME)
                                    .tags(ec2Tags)
                                    .build()))
                        .metadataOptions(
                            metadataOptionsBuilder ->
                                metadataOptionsBuilder
                                    .httpEndpoint(
                                        LaunchTemplateInstanceMetadataEndpointState.ENABLED)
                                    .httpTokens(imdsV2)
                                    .httpPutResponseHopLimit(1)
                                    .instanceMetadataTags(
                                        LaunchTemplateInstanceMetadataTagsState.ENABLED))
                        .monitoring(monitoringBuilder -> monitoringBuilder.enabled(true))
                        .userData(userdata)
                        .blockDeviceMappings(this.buildBlockDeviceMappings(amiId, ebsConfig)))
            .tagSpecifications(
                TagSpecification.builder()
                    .resourceType(ResourceType.LAUNCH_TEMPLATE)
                    .tags(ec2Tags)
                    .build())
            .build();
    return this.ec2Client.createLaunchTemplate(createLaunchTemplateRequest).launchTemplate();
  }

  private List<Tag> getEc2Tags(Map<String, String> tags) {
    return tags.entrySet().stream()
        .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
        .toList();
  }

  public void delete(String launchTemplateId) {
    this.ec2Client.deleteLaunchTemplate(request -> request.launchTemplateId(launchTemplateId));
  }

  public LaunchTemplate describe(String launchTemplateId) {
    try {
      List<LaunchTemplate> launchTemplates =
          this.ec2Client
              .describeLaunchTemplates(request -> request.launchTemplateIds(launchTemplateId))
              .launchTemplates();
      return launchTemplates.get(0);
    } catch (Ec2Exception ec2Exception) {
      if (ec2Exception
          .getMessage()
          .contains("launch templates specified in the request does not exist")) {
        throw new LaunchTemplateNotFoundException(launchTemplateId);
      } else {
        throw ec2Exception;
      }
    }
  }
}
