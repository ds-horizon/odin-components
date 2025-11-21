packer {
  required_plugins {
    docker = {
      source  = "github.com/hashicorp/docker"
      version = "1.1.2"
    }
  }
}

variables {
  artifact_name        = "${artifact_name}"
  artifact_version     = "${artifact_version}"
  base_image_name      = "${base_image_name}"
  base_image_tag       = "${base_image_tag}"
  registry             = "${registry}"
  setup_script         = "${setup_script}"
  setup_script_enabled = "${setup_script_enabled?c}"
  start_script         = "${start_script}"
  base_dir             = "${base_dir}"
  image_tag            = "${image_tag}"
}

source "docker" "amd64" {
  image             = "${r"${var.base_image_name}"}:${r"${var.base_image_tag}"}"
  commit            = true
  platform          = "linux/amd64"
  fix_upload_owner  = true
  changes = [
    "WORKDIR ${r"${var.base_dir}"}",
    "ENTRYPOINT [\"bash\", \"-c\", \"bash ./${r"${var.artifact_name}"}/${r"${var.start_script}"}\"]"
  ]
}

source "docker" "arm64" {
  image             = "${r"${var.base_image_name}"}:${r"${var.base_image_tag}"}"
  commit            = true
  platform          = "linux/arm64"
  fix_upload_owner  = true
  changes = [
    "WORKDIR ${r"${var.base_dir}"}",
    "ENTRYPOINT [\"bash\", \"-c\", \"bash ./${r"${var.artifact_name}"}/${r"${var.start_script}"}\"]"
  ]
}

build {
  sources = [
    "source.docker.amd64",
    "source.docker.arm64"
  ]

  provisioner "shell" {
    inline = [
      "mkdir -p ${r"${var.base_dir}"}"
    ]
  }
  provisioner "file" {
    source      = "${r"${var.artifact_name}"}"
    destination = "${r"${var.base_dir}"}/"
  }

  provisioner "shell" {
    environment_vars = [
      <#list environment_variables as key, value>
      "${key}=${value}"<#if key_has_next>,</#if>
      </#list>
    ]
    inline_shebang = "/bin/bash -e"
    inline = [
      "SETUP_FILE=${r"${var.base_dir}"}/${r"${var.artifact_name}"}/${r"${var.setup_script}"}",
      "if [[ -f $SETUP_FILE && ${r"${var.setup_script_enabled}"} == true ]]; then",
        "bash $SETUP_FILE",
      "fi"
    ]
  }

  post-processors {
    post-processor "docker-tag" {
      repository = "${r"${var.registry}"}/${r"${var.artifact_name}"}"
      tags       = ["${r"${var.image_tag}"}-${r"${source.name}"}"]
    }

    post-processor "docker-push" {
      keep_input_artifact = false
    }

    post-processor "manifest" {
      output     = "manifest.json"
      strip_path = true
      custom_data = {
        registry   = var.registry
        image_name = var.artifact_name
        image_tag  = var.image_tag
      }
    }
  }
}
