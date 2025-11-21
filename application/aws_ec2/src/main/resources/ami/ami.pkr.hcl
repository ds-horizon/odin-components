packer {
  required_plugins {
    amazon = {
      version = "1.3.2"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variables {
  region               = "${region}"
  vpc_id               = "${vpc_id}"
  subnet_id            = "${subnet_id}"
  security_group_ids   = "${security_group_ids}"
  artifact_name        = "${artifact_name}"
  artifact_version     = "${artifact_version}"
  account_ids          = "${account_ids}"
  setup_script         = "${setup_script}"
  setup_script_enabled = "${setup_script_enabled?c}"
  base_dir             = "${base_dir}"
  unique_id            = "${unique_id}"
}

locals {
  tags     = {
    Name                                     = var.artifact_name
    Version                                  = var.artifact_version
    "component:application:artifact_name"    = var.artifact_name
    "component:application:artifact_version" = var.artifact_version
    <#list tags as key, value>
    "${key}" = "${value}"
    </#list>
  }
}


<#list ami_configs as idempotency_sha, ami_config>
data "amazon-ami" "source_${ami_config.filters.architecture}" {
  region      = var.region
  filters = {
    <#list ami_config.filters as key, value>
    ${key} = "${value}"
    </#list>
    state = "available"
  }
  most_recent = true
  owners      = split(",", var.account_ids)
}

source "amazon-ebs" "${ami_config.filters.architecture}" {
  ami_name           = "${r"${var.artifact_name}"}-${r"${var.artifact_version}"}-${ami_config.filters.architecture}-${idempotency_sha}-${r"${var.unique_id}"}"
  ami_users          = split(",", var.account_ids)
  tags               = merge(local.tags, { "component:application:source_ami" = data.amazon-ami.source_${ami_config.filters.architecture}.id })
  snapshot_tags      = merge(local.tags, { "component:application:source_ami" = data.amazon-ami.source_${ami_config.filters.architecture}.id })
  run_volume_tags    = merge(local.tags, { "component:application:source_ami" = data.amazon-ami.source_${ami_config.filters.architecture}.id })
  run_tags           = merge(local.tags, { "component:application:source_ami" = data.amazon-ami.source_${ami_config.filters.architecture}.id })
  instance_type      = "${ami_config.buildInstanceType}"
  subnet_id          = var.subnet_id
  vpc_id             = var.vpc_id
  security_group_ids = split(",", var.security_group_ids)
  region             = var.region
  source_ami         = data.amazon-ami.source_${ami_config.filters.architecture}.id
  ssh_username       = "${ami_config.sshUser}"
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
    instance_metadata_tags      = "enabled"
  }
}
</#list>

build {
  sources = [
    <#list ami_configs?values as ami_config>
    "source.amazon-ebs.${ami_config.filters.architecture}"<#if ami_config_has_next>,</#if>
    </#list>
  ]

  provisioner "file" {
    source      = "${r"${var.artifact_name}"}.zip"
    destination = "/tmp/${r"${var.artifact_name}"}.zip"
  }

  provisioner "shell" {
    inline = [
      "sudo mkdir -p ${r"${var.base_dir}"}",
      "sudo unzip -q /tmp/${r"${var.artifact_name}"}.zip -d /tmp",
      "sudo rm /tmp/${r"${var.artifact_name}"}.zip",
      "sudo mv /tmp/${r"${var.artifact_name}"}/ ${r"${var.base_dir}"}/${r"${var.artifact_name}"}"
    ]
  }

  provisioner "shell" {
    environment_vars = [
      <#list environment_variables as key, value>
      "${key}=${value}"<#if key_has_next>,</#if>
      </#list>
    ]
    inline_shebang = "/usr/bin/bash -e"
    inline = [
      "SETUP_FILE=${r"${var.base_dir}"}/${r"${var.artifact_name}"}/${r"${var.setup_script}"}",
      "if [[ -f $SETUP_FILE && ${r"${var.setup_script_enabled}"} == true ]]; then",
        "sudo -E bash $SETUP_FILE",
      "fi"
    ]
  }

  post-processor "manifest" {
    output = "manifest.json"
    strip_path = true
  }
}
