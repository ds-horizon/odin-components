export AWS_MAX_ATTEMPTS=10
export AWS_DEFAULT_REGION=${region}

function manage_targets {
  # Deregister instances
  export IMS_TOKEN=$(curl --silent --request PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 3600")
  export INSTANCE_ID_URL=http://169.254.169.254/latest/meta-data/instance-id
  status=$(curl --silent -o id.txt -w "%{http_code}" -H "X-aws-ec2-metadata-token: $IMS_TOKEN" $INSTANCE_ID_URL)
  if [[ $status -ne 200 ]]; then
    echo "Failed to get instance id. Status code:$status"
    exit 1
  fi
  instance_id=$(cat id.txt)
  target_group_arns=(${target_group_arns})
  load_balancer_names=(${load_balancer_names})
  for i in ${r"${!target_group_arns[@]}"};
  do
    if [[ $1 == "deregister" ]]; then
      echo "Deregistering instance $instance_id from target group: ${r"${target_group_arns[i]}"}"
      aws elbv2 deregister-targets --target-group-arn ${r"${target_group_arns[i]}"} --targets Id=$instance_id || exit 1
    fi
    if [[ $1 == "register" ]]; then
      echo "Registering instance $instance_id to target group: ${r"${target_group_arns[i]}"}"
      aws elbv2 register-targets --target-group-arn ${r"${target_group_arns[i]}"} --targets Id=$instance_id || exit 1
    fi
  done


  for i in ${r"${!load_balancer_names[@]}"};
  do
    if [[ $1 == "deregister" ]]; then
      echo "Deregistering instance $instance_id from load balancer: ${r"${load_balancer_names[i]}"}"
      aws elb deregister-instances-from-load-balancer --load-balancer-name ${r"${load_balancer_names[i]}"} --instances $instance_id || exit 1
    fi
    if [[ $1 == "register" ]]; then
      echo "Registering instance $instance_id to load balancer: ${r"${load_balancer_names[i]}"}"
      aws elb register-instances-with-load-balancer --load-balancer-name ${r"${load_balancer_names[i]}"} --instances $instance_id || exit 1
    fi
  done
  echo "Waiting $2 seconds for target to $1"
  sleep $2
}


function cleanup {
  exit_code=$?
  manage_targets register ${time_to_wait_for_registration?c}
  exit $exit_code
}


trap "cleanup" EXIT
