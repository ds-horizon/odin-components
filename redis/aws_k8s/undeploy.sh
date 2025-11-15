#!/usr/bin/env bash
set -euo pipefail
# source ./logging.sh
# setup_error_handling

{
  # Environment variables
  export KUBECONFIG={{ componentMetadata.kubeConfigPath }}
  export RELEASE_NAME={{ componentMetadata.name }}
  export NAMESPACE={{ componentMetadata.envName }}
  export DEPLOYMENT_MODE={{ flavourConfig.deploymentMode | default('standalone') }}
  
  echo "================================================================"
  echo "Redis Undeployment - Opstree Operator"
  echo "================================================================"
  echo "Release Name: ${RELEASE_NAME}"
  echo "Namespace: ${NAMESPACE}"
  echo "Deployment Mode: ${DEPLOYMENT_MODE}"
  echo "================================================================"
  echo ""

  # Check if namespace exists
  if ! kubectl get namespace ${NAMESPACE} &>/dev/null; then
    echo "Namespace ${NAMESPACE} not found. Nothing to undeploy."
    exit 0
  fi

  # Delete Redis resources based on deployment mode
  case "${DEPLOYMENT_MODE}" in
    standalone)
      echo "Deleting Redis standalone instance..."
      kubectl delete redis ${RELEASE_NAME}-standalone -n ${NAMESPACE} --ignore-not-found=true --timeout=5m
      ;;
    sentinel)
      echo "Deleting Redis Sentinel..."
      kubectl delete redissentinel ${RELEASE_NAME}-sentinel -n ${NAMESPACE} --ignore-not-found=true --timeout=5m
      echo "Deleting Redis Replication..."
      kubectl delete redisreplication ${RELEASE_NAME}-replication -n ${NAMESPACE} --ignore-not-found=true --timeout=5m
      ;;
    cluster)
      echo "Deleting Redis Cluster..."
      kubectl delete rediscluster ${RELEASE_NAME}-cluster -n ${NAMESPACE} --ignore-not-found=true --timeout=5m
      ;;
    *)
      echo "WARNING: Unknown deployment mode: ${DEPLOYMENT_MODE}" 1>&2
      echo "Attempting to delete all possible Redis resources..." 1>&2
      kubectl delete redis,redisreplication,redissentinel,rediscluster -n ${NAMESPACE} --all --ignore-not-found=true --timeout=5m
      ;;
  esac

  # Wait for pods to terminate
  echo ""
  echo "Waiting for Redis pods to terminate..."
  kubectl wait --for=delete pod -l redis_setup_type -n ${NAMESPACE} --timeout=5m 2>/dev/null || true

  # Check if any Redis pods still exist
  REMAINING_PODS=$(kubectl get pods -n ${NAMESPACE} -l redis_setup_type --no-headers 2>/dev/null | wc -l)
  if [[ ${REMAINING_PODS} -gt 0 ]]; then
    echo "WARNING: ${REMAINING_PODS} Redis pod(s) still exist in namespace ${NAMESPACE}" 1>&2
    kubectl get pods -n ${NAMESPACE} -l redis_setup_type
  else
    echo "All Redis pods terminated successfully"
  fi

  # Delete PVCs (persistent volumes) - these are typically labeled with the instance name
  echo ""
  echo "Checking for Redis PersistentVolumeClaims..."
  REDIS_PVCS=$(kubectl get pvc -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | wc -l || echo "0")
  
  if [[ ${REDIS_PVCS} -gt 0 ]]; then
    echo "Found ${REDIS_PVCS} Redis PVC(s). Deleting..."
    kubectl get pvc -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | awk '{print $1}' | xargs -r kubectl delete pvc -n ${NAMESPACE} --timeout=5m || true
    echo "Redis PVCs deleted"
  else
    echo "No Redis PVCs found"
  fi

  # Delete Services - these are managed by the operator and tied to the CRD
  echo ""
  echo "Checking for Redis Services..."
  REDIS_SERVICES=$(kubectl get svc -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | wc -l || echo "0")
  
  if [[ ${REDIS_SERVICES} -gt 0 ]]; then
    echo "Found ${REDIS_SERVICES} Redis Service(s). Deleting..."
    kubectl get svc -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | awk '{print $1}' | xargs -r kubectl delete svc -n ${NAMESPACE} --timeout=2m || true
    echo "Redis Services deleted"
  else
    echo "No Redis Services found"
  fi

  # Delete ConfigMaps
  echo ""
  echo "Checking for Redis ConfigMaps..."
  REDIS_CONFIGMAPS=$(kubectl get configmap -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | wc -l || echo "0")
  
  if [[ ${REDIS_CONFIGMAPS} -gt 0 ]]; then
    echo "Found ${REDIS_CONFIGMAPS} Redis ConfigMap(s). Deleting..."
    kubectl get configmap -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | awk '{print $1}' | xargs -r kubectl delete configmap -n ${NAMESPACE} --timeout=1m || true
    echo "Redis ConfigMaps deleted"
  else
    echo "No Redis ConfigMaps found"
  fi

  # Delete Secrets (optional - be careful with this)
  echo ""
  echo "Checking for Redis Secrets..."
  REDIS_SECRETS=$(kubectl get secret -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | wc -l || echo "0")
  
  if [[ ${REDIS_SECRETS} -gt 0 ]]; then
    echo "Found ${REDIS_SECRETS} Redis Secret(s). Deleting..."
    kubectl get secret -n ${NAMESPACE} --no-headers 2>/dev/null | grep -E "${RELEASE_NAME}-(standalone|replication|sentinel|cluster)" | awk '{print $1}' | xargs -r kubectl delete secret -n ${NAMESPACE} --timeout=1m || true
    echo "Redis Secrets deleted"
  else
    echo "No Redis Secrets found"
  fi

  # Final status check
  echo ""
  echo "================================================================"
  echo "Undeployment Status"
  echo "================================================================"
  
  # Check for any remaining Redis resources
  REMAINING_RESOURCES=$(kubectl get redis,redisreplication,redissentinel,rediscluster -n ${NAMESPACE} --no-headers 2>/dev/null | wc -l)
  REMAINING_PODS=$(kubectl get pods -n ${NAMESPACE} -l redis_setup_type --no-headers 2>/dev/null | wc -l)
  
  if [[ ${REMAINING_RESOURCES} -eq 0 ]] && [[ ${REMAINING_PODS} -eq 0 ]]; then
    echo "✓ All Redis resources successfully removed from namespace ${NAMESPACE}"
  else
    echo "⚠ WARNING: Some Redis resources may still exist:" 1>&2
    if [[ ${REMAINING_RESOURCES} -gt 0 ]]; then
      echo "  - ${REMAINING_RESOURCES} Redis CRD resource(s)" 1>&2
      kubectl get redis,redisreplication,redissentinel,rediscluster -n ${NAMESPACE} 2>/dev/null || true
    fi
    if [[ ${REMAINING_PODS} -gt 0 ]]; then
      echo "  - ${REMAINING_PODS} Redis pod(s)" 1>&2
      kubectl get pods -n ${NAMESPACE} -l redis_setup_type 2>/dev/null || true
    fi
  fi
  
  echo ""
  echo "================================================================"
  echo "Undeployment completed!"
  echo "================================================================"
  echo ""
  echo "Note: The namespace '${NAMESPACE}' has NOT been deleted."
  echo "Note: The Opstree Redis Operator has NOT been uninstalled."
  echo ""
  echo "To manually delete the namespace, run:"
  echo "  kubectl delete namespace ${NAMESPACE}"
  echo ""
  echo "To uninstall the Opstree Redis Operator, run:"
  echo "  helm uninstall redis-operator -n redis-operator"
  echo ""

} #2> >(log_errors_with_timestamp) | log_with_timestamp

