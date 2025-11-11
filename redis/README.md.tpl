# odin-redis-component

Deploy and manage Redis on various platforms.

## Flavors
- [aws_elasticache](aws_elasticache) - Managed Redis using AWS ElastiCache
- [aws_k8s](aws_k8s) - Self-managed Redis on AWS EKS using OpsTree Operator
- [local_k8s](local_k8s) - Self-managed Redis on local Kubernetes (kind, k3s, minikube, Docker Desktop) using OpsTree Operator

{{ .Markdown 2 }}

## Running locally

* Update `example/*.json` accordingly
* Download DSL jar from [artifactory](https://dreamsports.jfrog.io/ui/repos/tree/General/d11-repo/com/dream11/odin-component-interface)
* Execute the following commands
```
  export PATH_TO_JAR=<path to downloaded jar>
  bash run.sh stage=<stage> operation=<operation> account_flavour=<account_flavour>
  example:
  bash run.sh stage=deploy account_flavour=dev_aws_elasticache
```

## Contributing

* Run `bash ../readme-generator.sh` from the component directory or `bash readme-generator.sh` from repository root to auto generate README
