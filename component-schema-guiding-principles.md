# Component Schema Guiding Principles

## Table of Contents
1. [Core Architecture: Definition vs Provisioning](#core-architecture-definition-vs-provisioning)
2. [The Liskov Substitution Principle (LSP)](#the-liskov-substitution-principle-lsp)
3. [Property Placement Decision Framework](#property-placement-decision-framework)
4. [Property Exposure Principles](#property-exposure-principles)
5. [Default Values Philosophy](#default-values-philosophy)
6. [Property Description Guidelines](#property-description-guidelines)
7. [API Mapping and Validation](#api-mapping-and-validation)
8. [Schema Merging and Cross-Schema Validation](#schema-merging-and-cross-schema-validation)
9. [Common Pitfalls and Lessons Learned](#common-pitfalls-and-lessons-learned)
10. [Documentation and README Generation](#documentation-and-readme-generation)

---

## Core Architecture: Definition vs Provisioning

### The Fundamental Separation

Every Odin component follows a strict separation of concerns:

```
Component/
├── schema.json          # WHAT: Component Definition
├── defaults.json        # Default values for definition
└── <flavour>/
    ├── schema.json      # HOW/WHERE: Provisioning specifics
    └── defaults.json    # Default values for provisioning
```

### Component Definition (Root Level)
**Purpose:** Defines WHAT the software is - its fundamental, logical properties

**Characteristics:**
- Properties that fundamentally change how clients interact with the component
- Settings that require different client libraries or connection logic
- Configuration that defines the core behavior of the software
- Properties that must be consistent across ALL flavours

**Examples:**
```json
{
  "redisVersion": "7.1",           // Determines client protocol compatibility
  "clusterModeEnabled": true,      // Changes connection logic fundamentally
  "authentication": {              // Affects how clients authenticate
    "enabled": true,
    "authToken": "secret"
  }
}
```

### Provisioning (Flavour Level)
**Purpose:** Defines HOW and WHERE the component runs

**Characteristics:**
- Platform-specific implementation details
- Operational tuning parameters
- Infrastructure configuration
- Settings that don't change the client interaction model

**Examples:**
```json
{
  "cacheNodeType": "cache.t4g.micro",     // AWS-specific instance type
  "securityGroupIds": ["sg-123"],         // AWS networking
  "snapshotRetentionLimit": 7,            // Operational backup config
  "multiAzEnabled": true                  // AWS availability config
}
```

---

## The Liskov Substitution Principle (LSP)

### Definition
**Any property in the root definition MUST be implementable across ALL flavours without breaking the contract.**

### Why LSP Matters

Properties in the root definition create a contract that ALL flavours must fulfill. If a flavour cannot support a root property, it violates LSP and breaks the component's abstraction.

### The Critical Test

Before adding a property to root definition, ask:
1. Can **every** flavour support this property?
2. Does the property work the same way across all platforms?
3. Are there hidden dependencies that might prevent configuration?

### Real-World LSP Violations

#### Example 1: Port Configuration
```json
// WRONG - In root definition
{
  "port": 6379  // ❌ ElastiCache fixes this to 6379, not configurable
}
```
**Why it fails:** Managed services like AWS ElastiCache don't allow port configuration

#### Example 2: Persistence Mode
```json
// WRONG - In root definition
{
  "persistenceMode": "aof"  // ❌ ElastiCache manages this internally
}
```
**Why it fails:** Cloud providers abstract away persistence configuration

#### Example 3: The Subtle Case - notifyKeyspaceEvents
```json
// APPEARS CORRECT but FAILS LSP
{
  "notifyKeyspaceEvents": "Ex"  // ❌ Requires custom parameter group in AWS
}
```
**Why it fails:**
- In AWS ElastiCache, this requires a custom parameter group
- Default parameter groups cannot be modified
- If user doesn't provide `parameterGroupName`, this property cannot be set
- Creates a hidden dependency, violating LSP

### LSP Validation Checklist

✅ **Property passes LSP if:**
- It can be configured in ALL flavours
- It works identically across platforms
- It has no hidden dependencies

❌ **Property fails LSP if:**
- Any flavour cannot support it
- It requires platform-specific workarounds
- It depends on other optional configurations
- Managed services abstract it away

---

## Property Placement Decision Framework

### The Decision Tree

```
┌─────────────────┐
│  New Property   │
└────────┬────────┘
         │
         ▼
┌──────────────────────┐     NO       ┌─────────────────────┐
│ Changes client       │─────────────►│ Provisioning/Flavour│
│ interaction model?   │              └─────────────────────┘
└──────────┬───────────┘                        ▲
           │ YES                                │
           ▼                                    │
┌──────────────────────┐     NO                 │
│ Is property truly    │────────────────────────┘
│ flavour-agnostic?    │
└──────────┬───────────┘                        ▲
           │ YES                                │
           ▼                                    │
┌──────────────────────┐     NO                 │
│ Supported by ALL     │────────────────────────┘
│ flavours?            │
└──────────┬───────────┘                        ▲
           │ YES                                │
           ▼                                    │
┌──────────────────────┐     NO                 │
│ No hidden            │────────────────────────┘
│ dependencies?        │
└──────────┬───────────┘
           │ YES
           ▼
┌──────────────────────┐
│   Root Definition    │
└──────────────────────┘
```

### Property Placement Examples

| Property | Location | Reasoning |
|----------|----------|-----------|
| `redisVersion` | Root | All flavours must support version selection |
| `clusterModeEnabled` | Root | Fundamentally changes client connection model |
| `cacheNodeType` | AWS Flavour | AWS-specific instance types |
| `port` | Self-managed Flavour only | Managed services fix this value |
| `notifyKeyspaceEvents` | Flavour | Requires platform-specific setup (parameter groups) |
| `securityGroupIds` | AWS Flavour | AWS-specific networking |
| `maxmemoryPolicy` | Flavour | Requires custom parameter group in AWS |
| `replicationGroupId` | Not Exposed | Platform-controlled for uniqueness |


## Property Exposure Principles

### Core Philosophy
**Not all API properties should be exposed to users. Some should be platform-controlled, others should have smart defaults, and only essential configuration should require user input.**

### Property Categories

#### 1. Platform-Controlled (Never Expose)
Properties that the platform should generate and manage internally:

**Characteristics:**
- Resource identifiers that must be unique
- Properties that could cause conflicts if user-controlled
- Internal implementation details
- Properties that enforce platform conventions

**Examples:**
```json
{
  "replicationGroupId": "auto-generated",  // Platform ensures uniqueness
  "clusterId": "auto-generated",           // Prevents naming conflicts
  "internalDnsName": "auto-generated"      // Platform-managed networking
}
```

**Why Not Expose:**
- Prevents naming conflicts across environments
- Enforces naming conventions
- Ensures uniqueness guarantees
- Hides infrastructure complexity
- Maintains platform consistency

#### 2. Smart Defaults (Expose but Provide Defaults)
Properties users CAN configure but usually shouldn't need to:

**Characteristics:**
- Have sensible defaults for 80% of use cases
- Optional for getting started
- Can be tuned for specific needs

**Examples:**
```json
{
  "replicationGroupDescription": "ElastiCache Redis replication group",
  "cacheNodeType": "cache.t4g.micro",
  "timeout": 0,
  "ipDiscovery": "ipv4"
}
```

#### 3. User-Required (Must Expose, No Default Possible)
Properties that are environment-specific and cannot have meaningful defaults:

**Characteristics:**
- Environment-specific configuration
- Security-sensitive settings
- Network/VPC configuration
- No universal default makes sense

**Examples:**
```json
{
  "cacheSubnetGroupName": null,  // User's VPC configuration
  "securityGroupIds": null,      // User's security setup
  "authToken": null,              // Secrets/passwords
  "vpcId": null                   // Infrastructure-specific
}
```

### Decision Framework for Property Exposure

```
┌─────────────────────┐
│   API Property      │
└──────────┬──────────┘
           │
           ▼
┌───────────────────────────┐     YES     ┌─────────────────────┐
│ Platform needs control    │────────────►│ DON'T EXPOSE        │
│ for uniqueness/convention?│             │ (Platform-generated)│
└───────────┬───────────────┘             └─────────────────────┘
            │ NO
            ▼
┌───────────────────────────┐     YES     ┌─────────────────────┐
│ Is it environment or      │────────────►│ EXPOSE              │
│ security specific?        │             │ (Required, no default)│
└───────────┬───────────────┘             └─────────────────────┘
            │ NO
            ▼
┌───────────────────────────┐     YES     ┌─────────────────────┐
│ Does 80% use case have    │────────────►│ EXPOSE with DEFAULT │
│ a common value?           │             │ (Optional property) │
└───────────┬───────────────┘             └─────────────────────┘
            │ NO
            ▼
┌───────────────────────────┐
│ EXPOSE as Required        │
│ (User must configure)     │
└───────────────────────────┘
```

### Examples of Property Exposure Decisions

| Property | Expose? | Default? | Reasoning |
|----------|---------|----------|-----------|
| `replicationGroupId` | ❌ No | N/A | Platform ensures uniqueness, prevents conflicts |
| `resourcePrefix` | ❌ No | N/A | Platform naming convention |
| `cacheSubnetGroupName` | ✅ Yes | ❌ No | Environment-specific VPC config |
| `securityGroupIds` | ✅ Yes | ❌ No | Security-specific to user's setup |
| `cacheNodeType` | ✅ Yes | ✅ Yes | Common starting point (t4g.micro) |
| `replicasPerNodeGroup` | ✅ Yes | ✅ Yes | Dev default (0), production can override |
| `authToken` | ✅ Yes | ❌ No | Security credential, user-specific |
| `autoMinorVersionUpgrade` | ✅ Yes | ✅ Yes | Security best practice (true) |

### Platform Control Benefits

When the platform controls certain properties:

1. **Consistency:** All resources follow naming conventions
2. **Safety:** No accidental conflicts or overwrites
3. **Traceability:** Can embed metadata (env, app, timestamp)
4. **Migration:** Platform can handle ID changes transparently
5. **Governance:** Enforce organizational policies automatically

### User Experience Optimization

The goal is to minimize required configuration while maintaining flexibility:

```yaml
# Minimal valid configuration (only required fields)
redis:
  redisVersion: "7.1"
  authentication:
    enabled: true
    authToken: "${SECRET}"
  discovery: "redis.myapp.internal"
  aws_elasticache:
    cacheSubnetGroupName: "my-subnet-group"
    securityGroupIds: ["sg-12345"]

# Everything else has smart defaults or is platform-controlled
```

## Default Values Philosophy

### Core Principle
**Defaults should enable quick starts with minimal configuration while guiding users toward production readiness through documentation.**

### Default Strategy

#### Development-Friendly Defaults
```json
{
  "replicasPerNodeGroup": 0,        // Minimize cost for dev
  "transitEncryptionEnabled": false, // Simplify client setup
  "snapshotRetentionLimit": 0,      // No backup costs
  "automaticFailoverEnabled": false // Simple single-node setup
}
```

#### But Security-First Where Critical
```json
{
  "autoMinorVersionUpgrade": true    // Security patches by default
}
```

### When to Provide Defaults

#### MUST Have Default When:
1. **Common Starting Point:** Most users want the same initial value
2. **Opt-in Features:** Safe to disable by default
3. **Best Practices:** Enables recommended settings
4. **Developer Experience:** Reduces initial configuration burden

#### MUST NOT Have Default When:
1. **Environment-Specific:** VPCs, subnets, security groups
2. **Secrets:** Passwords, tokens, keys
3. **Fundamental Choices:** Component identity (IDs, versions)
4. **Cost-Impacting:** Decisions with significant budget implications

### Smart Defaults Pattern

Transform API-required fields into optional fields through defaults:

```json
// AWS requires replicationGroupDescription
// But we provide a default to make it optional for users
{
  "replicationGroupDescription": {
    "type": "string",
    "default": "ElastiCache Redis replication group",
    "description": "Human-readable description..."
  }
}
```

---

## Property Description Guidelines

### Every description should explain:
1. **WHAT** - What is this property?
2. **WHY** - Why would you change it?
3. **IMPACT** - What happens when you change it?

### Description Template

```json
{
  "propertyName": {
    "description": "[WHAT it is]. [WHY you'd set it]. [IMPACT/TRADEOFFS]. **Default: `value`** ([why this default]). **Production:** [specific recommendation]."
  }
}
```

### Real Examples

#### Excellent Description
```json
{
  "transitEncryptionEnabled": {
    "description": "Enables TLS to encrypt data in transit between clients and the Redis server. Enable for production workloads handling sensitive data. Note: may impact performance by 10-20%. **Default: `false`** (disabled to simplify client configuration for development/testing). **Production:** Enable for sensitive data or compliance requirements."
  }
}
```

#### Poor Description
```json
{
  "transitEncryptionEnabled": {
    "description": "Enable transit encryption"  // ❌ No context, impact, or guidance
  }
}
```

### Production Guidance Requirements

Always include:
- Specific recommendations for production use
- Performance implications (latency, throughput impact)
- Cost implications (resource multiplication, storage costs)
- Security considerations
- Scaling guidance

---

## Cloud Provider API Mapping and Validation
This section is applicable for flavours that encapsulates cloud vendor offerings e.g. Redis - AWS Elasticache.
### Cloud Provider API Verification Requirements

Before adding ANY property to a flavour schema:

1. **Keep The Exact API Parameter Name But In camelCase**
   ```
   Cloud Vendor's API Parameter: CacheParameterGroupName
   Schema Property: parameterGroupName ❌, should be cacheParameterGroupName
   ```

2. **Follow Same Validate Constraints As of Cloud Vendor**
   ```json
   {
     "replicationGroupId": {
       "pattern": "^[a-z][a-z0-9\\-]{0,39}$",  // Must match API requirements
       "maxLength": 40
     }
   }
   ```

---

## Schema Merging and Cross-Schema Validation

### How Validation Works

**Important**: Root schema and flavor schema are **merged before validation**. This is implemented in the `odin-component-interface` and allows flavor schemas to reference and validate against root schema properties.

### Validation Flow

```
┌─────────────────────────────────────────────────────────────┐
│  Step 1: User Configuration                                 │
│  ├── Root properties (redis/schema.json)                    │
│  │   └── clusterModeEnabled: true                           │
│  └── Flavor properties (redis/aws_container/schema.json)    │
│      └── deploymentMode: "cluster"                          │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  Step 2: Schema Merging (odin-component-interface)          │
│  Combines root and flavor schemas into single merged schema │
│  {                                                           │
│    "clusterModeEnabled": true,        // From root          │
│    "deploymentMode": "cluster",       // From flavor        │
│    "cluster": { ... },                // From flavor        │
│    ... all other properties                                 │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  Step 3: Validation (JSON Schema)                           │
│  Validates merged configuration against merged schema       │
│  - Root schema validations apply                            │
│  - Flavor schema validations apply                          │
│  - Cross-schema validations apply (allOf conditions)        │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  Result: Valid or Error                                     │
│  If invalid, returns error message from failed validation   │
└─────────────────────────────────────────────────────────────┘
```

### Cross-Schema Dependencies

Since schemas are merged, flavor schemas can reference root schema properties directly in validation rules. This enables enforcing dependencies between root and flavor properties.

#### Example: Cluster Mode Dependency

**Root Schema (redis/schema.json):**
```json
{
  "properties": {
    "clusterModeEnabled": {
      "type": "boolean",
      "description": "Enable Redis Cluster mode for horizontal scaling",
      "default": false
    }
  }
}
```

**Flavor Schema (redis/aws_container/schema.json):**
```json
{
  "allOf": [
    {
      "if": {
        "properties": {
          "deploymentMode": { "const": "cluster" }
        }
      },
      "then": {
        "properties": {
          "clusterModeEnabled": { "const": true }  // <-- References root property
        },
        "errorMessage": "deploymentMode: 'cluster' requires clusterModeEnabled: true in root schema"
      }
    },
    {
      "if": {
        "properties": {
          "clusterModeEnabled": { "const": false }  // <-- References root property
        }
      },
      "then": {
        "properties": {
          "deploymentMode": {
            "enum": ["standalone", "sentinel"]
          }
        },
        "errorMessage": "clusterModeEnabled: false does not support deploymentMode: 'cluster'"
      }
    }
  ]
}
```

**How It Works:**
1. User provides configuration with both root and flavor properties
2. Schemas are merged: `clusterModeEnabled` (root) + `deploymentMode` (flavor)
3. Validation runs on merged schema
4. Flavor's `allOf` rules can check `clusterModeEnabled` value
5. If mismatch detected, validation fails with clear error message

#### Real-World Example: ElastiCache

**From redis/aws_elasticache/schema.json:**
```json
{
  "allOf": [
    {
      "if": {
        "properties": {
          "numNodeGroups": { "minimum": 2 }  // Flavor property
        }
      },
      "then": {
        "properties": {
          "clusterModeEnabled": { "const": true }  // Root property reference
        },
        "errorMessage": "numNodeGroups can only be greater than 1 when clusterModeEnabled is true"
      }
    }
  ]
}
```

This validates that ElastiCache's `numNodeGroups` (flavor property) aligns with Redis's `clusterModeEnabled` (root property).

### When to Use Cross-Schema Validation

Use cross-schema validation when:

1. **Enforcing LSP**: Ensure flavor implementations respect root schema contracts
2. **Preventing Misconfigurations**: Catch incompatible property combinations early
3. **Clear Error Messages**: Guide users to correct configuration issues
4. **Platform-Specific Requirements**: Validate platform constraints against logical configuration

### Best Practices for Cross-Schema Validation

#### 1. Always Validate Both Directions

```json
{
  "allOf": [
    // Direction 1: Flavor property requires root property
    {
      "if": { "properties": { "flavorProp": { "const": "value" } } },
      "then": { "properties": { "rootProp": { "const": true } } }
    },
    // Direction 2: Root property constrains flavor property
    {
      "if": { "properties": { "rootProp": { "const": false } } },
      "then": { "properties": { "flavorProp": { "not": { "const": "value" } } } }
    }
  ]
}
```

#### 2. Provide Clear Error Messages

```json
{
  "errorMessage": "deploymentMode: 'cluster' requires clusterModeEnabled: true in root schema"
  // ✅ GOOD: Tells user exactly what's wrong and which schemas are involved
}
```

```json
{
  "errorMessage": "Invalid configuration"
  // ❌ BAD: Doesn't explain what's wrong or how to fix it
}
```

#### 3. Document Cross-Schema Dependencies

In property descriptions, explicitly mention dependencies on root schema:

```json
{
  "deploymentMode": {
    "description": "... **IMPORTANT:** Cluster mode requires `clusterModeEnabled: true` in root schema ..."
  }
}
```

#### 4. Test All Combinations

When adding cross-schema validations, test:

| Root Property | Flavor Property | Expected Result |
|--------------|----------------|-----------------|
| `true` | Compatible value | ✅ Valid |
| `true` | Incompatible value | ❌ Error |
| `false` | Compatible value | ✅ Valid |
| `false` | Incompatible value | ❌ Error |

### Common Cross-Schema Validation Patterns

#### Pattern 1: Mandatory Dependency
```json
// Flavor property REQUIRES root property
{
  "if": { "properties": { "flavorFeature": { "const": true } } },
  "then": { "properties": { "rootFeature": { "const": true } } },
  "errorMessage": "flavorFeature requires rootFeature: true in root schema"
}
```

#### Pattern 2: Mutual Exclusivity
```json
// Root property PREVENTS flavor property
{
  "if": { "properties": { "rootMode": { "const": "simple" } } },
  "then": {
    "properties": {
      "flavorAdvancedFeature": { "const": false }
    }
  },
  "errorMessage": "rootMode: 'simple' does not support flavorAdvancedFeature"
}
```

#### Pattern 3: Conditional Requirements
```json
// Flavor property requires additional flavor properties when root property is set
{
  "if": {
    "properties": {
      "rootFeature": { "const": true },
      "flavorMode": { "const": "advanced" }
    }
  },
  "then": {
    "required": ["flavorConfig"],
    "properties": {
      "flavorConfig": { "type": "object" }
    }
  },
  "errorMessage": "rootFeature: true with flavorMode: 'advanced' requires flavorConfig"
}
```

### Validation Error Messages

When validation fails, users see error messages that include:
1. **Which property failed**: The specific property that violated the constraint
2. **Why it failed**: The validation rule that was violated
3. **How to fix it**: Clear guidance from the `errorMessage` field

**Example Error Output:**
```
Validation Error: Configuration invalid

Property: deploymentMode
Value: "cluster"
Error: deploymentMode: 'cluster' requires clusterModeEnabled: true in root schema

Fix: Set clusterModeEnabled: true in the root Redis configuration
```

### Debugging Cross-Schema Validations

If validations aren't working as expected:

1. **Check Schema Merging**: Verify both schemas are being merged correctly
2. **Test Merged Schema**: Validate against the merged schema directly
3. **Check Property Names**: Ensure root property names match exactly (case-sensitive)
4. **Review allOf Order**: Later rules can override earlier ones
5. **Use errorMessage**: Add detailed error messages to debug which rule is triggering

### Example: Complete Cross-Schema Validation

```json
{
  "title": "AWS Container Flavor",
  "allOf": [
    // Validate cluster mode dependency
    {
      "if": {
        "properties": { "deploymentMode": { "const": "cluster" } }
      },
      "then": {
        "properties": { "clusterModeEnabled": { "const": true } },
        "required": ["cluster"],
        "errorMessage": "deploymentMode: 'cluster' requires clusterModeEnabled: true and cluster configuration"
      }
    },
    // Prevent cluster mode when not enabled
    {
      "if": {
        "properties": { "clusterModeEnabled": { "const": false } }
      },
      "then": {
        "properties": {
          "deploymentMode": { "enum": ["standalone", "sentinel"] }
        },
        "errorMessage": "clusterModeEnabled: false only supports standalone or sentinel modes"
      }
    },
    // Validate high availability requirements
    {
      "if": {
        "properties": {
          "deploymentMode": { "const": "sentinel" }
        }
      },
      "then": {
        "properties": { "replicaCount": { "minimum": 1 } },
        "errorMessage": "Sentinel mode requires at least 1 replica for high availability"
      }
    }
  ],
  "properties": {
    "deploymentMode": {
      "type": "string",
      "enum": ["standalone", "sentinel", "cluster"],
      "description": "Deployment topology. Cluster mode requires clusterModeEnabled: true in root schema."
    }
  }
}
```

---

## Common Pitfalls and Lessons Learned

### 1. The Hidden Dependency Trap

**Problem:** Property seems universal but requires platform-specific setup
```json
// notifyKeyspaceEvents of Redis requires custom parameter groups in AWS
// Default parameter groups cannot be modified!
```

**Lesson:** Always test with DEFAULT configurations, not just custom ones

### 2. The Managed Service Abstraction

**Problem:** Assuming managed services work like self-hosted
```
Self-hosted Redis: Full control over redis.conf
ElastiCache: Many settings managed internally
```

**Lesson:** Managed services are opinionated; respect their constraints

### 3. The Cost Surprise

**Problem:** Production-safe defaults that surprise with costs
```json
{
  "replicasPerNodeGroup": 2,  // 3x the cost!
  "multiAzEnabled": true       // Additional charges
}
```

**Lesson:** Dev-friendly defaults with production guidance in descriptions

### 4. The Version Dependency

**Problem:** Features requiring specific versions
```json
{
  "ipDiscovery": "ipv6"  // Requires Redis 6.2+
}
```

**Lesson:** Document version requirements clearly

---

## Documentation and README Generation

### Auto-Generated Documentation

Components use `readme-generator.sh` to create documentation from schemas:

```bash
#!/bin/bash
# Run from repository root after any schema changes
bash readme-generator.sh
# This will recursively generate READMEs for all components and their flavors
```

### Documentation Structure

```
Component/
├── README.md.tpl         # Template with static content
├── README.md            # Generated (do not edit directly)
├── schema.json          # Source of property tables
├── defaults.json        # Source of default values
└── <flavour>/
    ├── README.md.tpl    # Flavour-specific template
    └── README.md        # Generated flavour docs
```

### README Template Best Practices

1. **Component Overview**: Clear explanation of what the component does
2. **Quick Start**: Minimal configuration example
3. **Property Tables**: Auto-generated from schemas
4. **Production Guide**: Security and scaling recommendations
5. **Examples**: Common configuration patterns
6. **Troubleshooting**: Known issues and solutions

### Property Table Generation

The generator creates tables from schema:
- Property name and type
- Required/Optional status
- Default values from defaults.json
- Descriptions from schema
- Validation constraints

### Maintaining Documentation

1. **Never edit README.md directly** - Edit templates and schemas
2. **Run generator after schema changes** - Keep docs in sync
3. **Update templates for structural changes** - Add new sections as needed
4. **Include examples** - Show real-world usage patterns

---

## Real-World Example - Redis Component Analysis

### Properties That Failed LSP Testing

| Property | Why It Failed | Lesson |
|----------|--------------|--------|
| `port` | ElastiCache fixes to 6379 | Managed services abstract infrastructure |
| `persistenceMode` | Not configurable in ElastiCache | Cloud providers manage persistence internally |
| `databases` | Not documented as configurable | Don't assume all Redis features are exposed |
| `notifyKeyspaceEvents` | Requires custom parameter group | Hidden dependencies break LSP |

### Properties Successfully Placed

| Property | Location | Why |
|----------|----------|-----|
| `redisVersion` | Root | Fundamental to client compatibility |
| `clusterModeEnabled` | Root | Changes connection model |
| `replicationGroupId` | AWS Flavour | AWS-specific identifier |
| `cacheNodeType` | AWS Flavour | Platform-specific resource types |
