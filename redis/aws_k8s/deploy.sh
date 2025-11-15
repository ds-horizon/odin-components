#!/usr/bin/env bash
set -euo pipefail

{
  # Environment variables
  export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
  export RELEASE_NAME={{ componentMetadata.name }}
  export NAMESPACE={{ componentMetadata.envName }}
  export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode | default('standalone') }}

  echo "================================================================"
  echo "Redis Deployment - Opstree Operator"
  echo "================================================================"
  echo "Release Name: ${RELEASE_NAME}"
  echo "Namespace: ${NAMESPACE}"
  echo "Deployment Mode: ${DEPLOYMENT_MODE}"
  echo "================================================================"

  # Create namespace if it doesn't exist
  kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
  
  # Check if Opstree Redis Operator is installed
  echo "Checking Opstree Redis Operator installation..."
  if ! kubectl get deployment redis-operator -n redis-operator &>/dev/null; then
    echo "Opstree Redis Operator not found. Installing..."
    helm repo add ot-helm https://ot-container-kit.github.io/helm-charts/ --force-update
    helm repo update
    helm upgrade --install redis-operator ot-helm/redis-operator \
      --namespace redis-operator \
      --create-namespace \
      --wait \
      --timeout 5m
    echo "Opstree Redis Operator installed successfully"
  else
    echo "Opstree Redis Operator already installed"
  fi

  # Select the appropriate values file based on deployment mode
  case "${DEPLOYMENT_MODE}" in
    standalone)
      VALUES_FILE="standalone-mode.yaml"
      ;;
    sentinel)
      VALUES_FILE="sentinel-mode.yaml"
      ;;
    cluster)
      VALUES_FILE="cluster-mode.yaml"
      ;;
    *)
      echo "ERROR: Unknown deployment mode: ${DEPLOYMENT_MODE}" 1>&2
      echo "Supported modes: standalone, sentinel, cluster" 1>&2
      exit 1
      ;;
  esac

  # Check if values file exists
  if [[ ! -f "${VALUES_FILE}" ]]; then
    echo "ERROR: Values file not found: ${VALUES_FILE}" 1>&2
    exit 1
  fi

  # Generate the final manifest from template using envsubst
  echo "Generating Redis manifest for ${DEPLOYMENT_MODE} mode..."
  TEMP_MANIFEST="temp-redis-manifest-${DEPLOYMENT_MODE}.yaml"
  
  # Replace placeholders in the values file

  envsubst < "${VALUES_FILE}" > "${TEMP_MANIFEST}" #2> >(log_errors_with_timestamp) | log_with_timestamp

  # Apply the manifest
  echo "Applying Redis ${DEPLOYMENT_MODE} configuration..."
  kubectl apply -f "${TEMP_MANIFEST}"
  
  # Clean up temp file
  rm -f "${TEMP_MANIFEST}"

  # Function to wait for CRD and pods
  wait_for_resource() {
    local crd_type=$1
    local resource_name=$2
    local app_label=$3
    local display_name=$4
    
    echo "Waiting for ${display_name}..."
    
    # Wait for StatefulSet to be created (operator creates this)
    local max_wait=300
    local count=0
    while [[ $count -lt $max_wait ]]; do
      if kubectl get statefulset -n ${NAMESPACE} -l app=${app_label} &>/dev/null; then
        echo "StatefulSet created"
        break
      fi
      sleep 2
      count=$((count + 1))
    done
    
    # Give operator time to create pods from StatefulSet
    echo "Waiting for pods to be created..."
    count=0
    max_wait=60  # Wait up to 2 minutes for pods to appear
    while [[ $count -lt $max_wait ]]; do
      local pod_count=$(kubectl get pods -n ${NAMESPACE} -l app=${app_label} --no-headers 2>/dev/null | wc -l)
      if [[ ${pod_count} -gt 0 ]]; then
        echo "Pods created (${pod_count} pod(s) found)"
        break
      fi
      sleep 2
      count=$((count + 1))
    done
    
    # Wait for pods to become ready (15 min timeout for autoscaling/image pull)
    echo "Waiting for pods to become ready..."
    kubectl wait --for=condition=ready pod \
      -l app=${app_label} \
      -n ${NAMESPACE} \
      --timeout=15m 2>&1 || {
        echo "Some pods may still be starting up..."
      }
    
    # Check if pods are running
    local ready_pods=$(kubectl get pods -n ${NAMESPACE} \
      -l app=${app_label} \
      --field-selector=status.phase=Running --no-headers 2>/dev/null | wc -l)
    
    if [[ ${ready_pods} -eq 0 ]]; then
      echo "WARNING: ${display_name} pods not ready yet. Please check status manually." 1>&2
      return 1
    else
      echo "${display_name} deployed successfully (${ready_pods} pod(s) running)"
      return 0
    fi
  }

  # Wait for resources to be ready based on deployment mode
  echo "Waiting for Redis deployment to be ready..."
  
  case "${DEPLOYMENT_MODE}" in
    standalone)
      wait_for_resource "redis" "${RELEASE_NAME}-standalone" "${RELEASE_NAME}-standalone" "Redis standalone instance"
      ;;
    sentinel)
      wait_for_resource "redisreplication" "${RELEASE_NAME}-replication" "${RELEASE_NAME}-replication" "Redis Replication"
      wait_for_resource "redissentinel" "${RELEASE_NAME}-sentinel" "${RELEASE_NAME}-sentinel" "Redis Sentinel"
      ;;
    cluster)
      # For cluster mode, wait for leader pods first
      wait_for_resource "rediscluster" "${RELEASE_NAME}-cluster" "${RELEASE_NAME}-cluster-leader" "Redis Cluster Leaders"
      # Wait for follower pods
      wait_for_resource "rediscluster" "${RELEASE_NAME}-cluster" "${RELEASE_NAME}-cluster-follower" "Redis Cluster Followers"
      ;;
  esac

  # Display deployment status
  echo ""
  echo "================================================================"
  echo "Deployment Status"
  echo "================================================================"
  kubectl get pods -n ${NAMESPACE} -o wide
  echo ""
  kubectl get svc -n ${NAMESPACE}
  echo ""
  
  # Show Redis CRDs status and connection information
  case "${DEPLOYMENT_MODE}" in
    standalone)
      kubectl get redis -n ${NAMESPACE} 2>/dev/null || echo "No Redis standalone resources found"
      echo ""
      echo "Connection Information:"
      echo "  Service: ${RELEASE_NAME}-standalone.${NAMESPACE}.svc.cluster.local:6379"
      ;;
    sentinel)
      kubectl get redisreplication -n ${NAMESPACE} 2>/dev/null || echo "No RedisReplication resources found"
      kubectl get redissentinel -n ${NAMESPACE} 2>/dev/null || echo "No RedisSentinel resources found"
      echo ""
      echo "Connection Information:"
      echo "  Sentinel Service: ${RELEASE_NAME}-sentinel.${NAMESPACE}.svc.cluster.local:26379"
      echo "  Redis Service: ${RELEASE_NAME}-replication.${NAMESPACE}.svc.cluster.local:6379"
      ;;
    cluster)
      kubectl get rediscluster -n ${NAMESPACE} 2>/dev/null || echo "No RedisCluster resources found"
      echo ""
      echo "Connection Information:"
      echo "  Cluster Service: ${RELEASE_NAME}-cluster.${NAMESPACE}.svc.cluster.local:6379"
      ;;
  esac
  
  echo ""
  echo "================================================================"
  echo "Deployment completed!"
  echo "================================================================"

}