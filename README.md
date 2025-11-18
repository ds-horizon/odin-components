# Odin Components Repository

## Overview

This repository contains the source code for various Odin components - production-grade infrastructure components designed for cloud-native deployments. Each component follows the [Odin Component Interface (OCI)](https://github.com/ds-horizon/odin-component-interface) specification to ensure consistency, reliability, and ease of use across different cloud platforms.

## Repository Structure

```
odin-components/
├── component-schema-guiding-principles.md  # Essential reading for contributors
└── component/                              # Component implementation
    └── flavour/                            # Component's flavour implementation
    └── component.json                      # Component's description and version
```

## For Component Developers

**📚 Required Reading:** Before creating or modifying components, please read:
- [**Component Schema Guiding Principles**](./component-schema-guiding-principles.md) - Comprehensive guide on schema design, property placement, LSP validation, and lessons learned

### Executing Component
* Download DSL jar from [odin-component-interface](https://github.com/ds-horizon/odin-component-interface/releases)
* Execute the following commands
  ```shell
  # Environment variables needed for component execution. Update the json accordingly
  export ODIN_BASE_CONFIG='{}' # Base config
  export ODIN_FLAVOUR_CONFIG='{}' # Flavour config
  export ODIN_OPERATION_CONFIG='{}' # Operation config
  export ODIN_COMPONENT_METADATA='{}' # Component metadata
  export ODIN_DSL_METADATA='{}' # DSL metadata
  groovy -cp ${PATH_TO_DSL_JAR} component.groovy

  ```

## Key Concepts

### Component Definition vs Provisioning
- **Root Level (`schema.json`)**: Defines WHAT the component is (e.g., Redis version, authentication)
- **Flavour Level (`<flavour>/schema.json`)**: Defines HOW/WHERE it runs (e.g., AWS instance types, networking)

### Design Principles
- **Developer-friendly defaults** for quick starts
- **Production guidance** embedded in descriptions
- **Strict LSP (Liskov Substitution Principle)** validation for root properties
- **API compatibility** verification for cloud provider flavours


## Contributing

1. Read the [Component Schema Guiding Principles](./component-schema-guiding-principles.md)
2. Follow the property placement decision framework
3. Validate against LSP requirements
4. Verify cloud provider API compatibility
5. Regenerate documentation after schema changes

## Learn More

- **OCI Specification**: [github.com/ds-horizon/odin-component-interface](https://github.com/ds-horizon/odin-component-interface)
- **Schema Design**: See our comprehensive [guiding principles document](./component-schema-guiding-principles.md)
