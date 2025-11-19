package com.dream11.application.client;

import com.dream11.application.constant.CanaryPhase;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategyBuilder;
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment;
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sClient implements AutoCloseable {

  final KubernetesClient kubernetesClient;

  public K8sClient() {
    this.kubernetesClient = new KubernetesClientBuilder().build();
  }

  public void createNamespace(
      String name, Map<String, String> labels, Map<String, String> annotations) {
    this.kubernetesClient
        .namespaces()
        .resource(
            new NamespaceBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(labels)
                .withAnnotations(annotations)
                .endMetadata()
                .build())
        .serverSideApply();
  }

  public Optional<Namespace> getNamespace(String name) {
    return Optional.ofNullable(this.kubernetesClient.namespaces().withName(name).get());
  }

  public void createSecret(String name, String type, Map<String, String> data, String namespace) {
    this.kubernetesClient
        .secrets()
        .inNamespace(namespace)
        .resource(
            new SecretBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withData(data)
                .withType(type)
                .build())
        .create();
  }

  public Optional<Secret> getSecret(String name, String namespace) {
    return Optional.ofNullable(
        this.kubernetesClient.secrets().inNamespace(namespace).withName(name).get());
  }

  public Optional<Deployment> getDeployment(String deploymentName, String namespace) {
    return Optional.ofNullable(
        this.kubernetesClient
            .apps()
            .deployments()
            .inNamespace(namespace)
            .withName(deploymentName)
            .get());
  }

  public int getReadyReplicaCount(String deploymentName, String namespace) {
    Integer availableReplicas =
        this.getDeployment(deploymentName, namespace).get().getStatus().getReadyReplicas();
    return Objects.nonNull(availableReplicas) ? availableReplicas : 0;
  }

  public void scaleDeployment(String deploymentName, String namespace, int replicas) {
    this.kubernetesClient
        .apps()
        .deployments()
        .inNamespace(namespace)
        .withName(deploymentName)
        .scale(replicas);
  }

  public void restartDeployment(
      String deploymentName,
      String namespace,
      int batchSizePercentage,
      Map<String, String> annotations) {
    RollingUpdateDeployment rollingUpdate =
        new RollingUpdateDeploymentBuilder()
            .withMaxUnavailable(new IntOrString(batchSizePercentage + "%"))
            .withMaxSurge(new IntOrString(1))
            .build();

    DeploymentStrategy strategy =
        new DeploymentStrategyBuilder()
            .withType("RollingUpdate")
            .withRollingUpdate(rollingUpdate)
            .build();

    this.kubernetesClient
        .apps()
        .deployments()
        .inNamespace(namespace)
        .withName(deploymentName)
        .edit(
            deployment ->
                new DeploymentBuilder(deployment)
                    .editSpec()
                    .withStrategy(strategy)
                    .endSpec()
                    .editMetadata()
                    .addToAnnotations(annotations)
                    .endMetadata()
                    .build());

    this.kubernetesClient
        .apps()
        .deployments()
        .inNamespace(namespace)
        .withName(deploymentName)
        .rolling()
        .restart();
  }

  public Optional<GenericKubernetesResource> getCanary(String canaryName, String namespace) {
    try {
      return Optional.ofNullable(
          this.kubernetesClient
              .genericKubernetesResources(
                  new ResourceDefinitionContext.Builder()
                      .withGroup("flagger.app")
                      .withVersion("v1beta1")
                      .withPlural("canaries")
                      .withKind("Canary")
                      .withNamespaced(true)
                      .build())
              .inNamespace(namespace)
              .withName(canaryName)
              .get());
    } catch (Exception e) {
      log.debug("Canary resource not found or error accessing: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public Optional<CanaryPhase> getCanaryPhase(String canaryName, String namespace) {
    return this.getCanary(canaryName, namespace)
        .flatMap(
            canary -> {
              Object statusObj = canary.getAdditionalProperties().get("status");
              if (statusObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> status = (Map<String, Object>) statusObj;
                if (status != null && status.containsKey("phase")) {
                  String phaseString = (String) status.get("phase");
                  return CanaryPhase.fromString(phaseString);
                }
              }
              return Optional.empty();
            });
  }

  @Override
  public void close() {
    if (this.kubernetesClient != null) {
      this.kubernetesClient.close();
    }
  }
}
