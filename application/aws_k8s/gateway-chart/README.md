# Gateway Chart

This is a wrapper chart for the Istio Gateway that adds custom AWS ALB ingress resources.

## Overview

This chart:
1. Uses the official `istio/gateway` chart as a dependency
2. Overrides the service type to `LoadBalancer`
3. Adds custom Kubernetes Gateway resource
4. Adds AWS ALB Ingress resources (external and internal)

## Structure

- **Dependency**: `istio/gateway` v1.27.2 from Istio's Helm repository
- **Custom Templates**:
  - `gateway.yaml` - Kubernetes Gateway API resource
  - `ingress-ext.yaml` - External ALB Ingress
  - `ingress-int.yaml` - Internal ALB Ingress

## Installation

Before installing, you need to update the dependencies:

```bash
cd gateway-chart
helm dependency update
```

This will download the `istio/gateway` chart into the `charts/` directory.

Then install:

```bash
helm install gateway . -n <namespace> -f values.yaml
```

## Configuration

### Istio Gateway Configuration

All istio-gateway configuration is passed through the `istio-gateway` key:

```yaml
istio-gateway:
  _internal_defaults_do_not_set:
    service:
      type: LoadBalancer  # Override from default ClusterIP
    # ... any other istio/gateway values
```

### Custom Ingress Configuration

```yaml
ingress:
  external:
    enabled: true  # Enable/disable external ALB
    annotations:
      # Custom ALB annotations for external ingress
      alb.ingress.kubernetes.io/tags: "..."
      service.beta.kubernetes.io/aws-load-balancer-security-groups: "..."
  internal:
    enabled: true  # Enable/disable internal ALB
    annotations:
      # Custom ALB annotations for internal ingress
      alb.ingress.kubernetes.io/tags: "..."
      service.beta.kubernetes.io/aws-load-balancer-security-groups: "..."

labels: {}  # Labels applied to Gateway and Ingress resources
```

## How It Works

1. The chart declares `istio/gateway` as a dependency with alias `istio-gateway`
2. All values under `istio-gateway.*` are passed to the dependency
3. Custom templates in this chart create additional resources:
   - A Kubernetes Gateway API resource
   - AWS ALB Ingress resources for external/internal access
4. The custom ingresses point to the service created by the istio-gateway subchart

## Updating Istio Version

To update to a new Istio version:

1. Edit `Chart.yaml` and update the dependency version
2. Run `helm dependency update`
3. Update the `version` and `appVersion` in `Chart.yaml`

## Development

The diff from the original `istio/gateway` chart includes:
- Service type changed from `ClusterIP` to `LoadBalancer`
- Added custom Gateway API resource
- Added AWS ALB Ingress resources (external + internal)
- Added ingress configuration to values

All other functionality comes from the upstream `istio/gateway` chart.
