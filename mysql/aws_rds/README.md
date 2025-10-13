## aws_rds Flavour

Deploy and perform operations on your application in AWS RDS

## Operations
- [add-readers](operations/add-readers)
- [remove-readers](operation/remove-readers)

### aws_rds provisioning configuration

#### Properties

| Property                           | Type                                        | Required | Description                                                                                    |
|------------------------------------|---------------------------------------------|----------|------------------------------------------------------------------------------------------------|
| `engineVersion`                    | string                                      | **Yes**  | Aurora MySQL engine version, e.g. 3.04.2                                                       |
| `writer`                           | [object](#writer)                           | **Yes**  | Writer DB instance specific configuration                                                      |
| `backtrackWindow`                  | integer                                     | No       | Aurora Backtrack window in seconds (if supported by engine version)                            |
| `backupRetentionPeriod`            | integer                                     | No       |                                                                                                |
| `clusterParameterGroupConfig`      | [object](#clusterparametergroupconfig)      | No       | Inline cluster parameter overrides (Aurora MySQL)                                              |
| `clusterParameterGroupName`        | string                                      | No       | Use an existing cluster parameter group                                                        |
| `copyTagsToSnapshot`               | boolean                                     | No       |                                                                                                |
| `credentials`                      | [object](#credentials)                      | No       | Choose static username/password OR Secrets Manager–managed                                     |
| `dbName`                           | string                                      | No       | Initial database name                                                                          |
| `deletionConfig`                   | [object](#deletionconfig)                   | No       | Preferences used when deleting this cluster                                                    |
| `deletionProtection`               | boolean                                     | No       |                                                                                                |
| `enableCloudwatchLogsExports`      | string[]                                    | No       |                                                                                                |
| `enableIAMDatabaseAuthentication`  | boolean                                     | No       | Enable IAM DB authentication for the cluster                                                   |
| `encryptionAtRest`                 | boolean                                     | No       | Controls cluster storage encryption                                                            |
| `globalClusterIdentifier`          | string                                      | No       | Join/create Global Database                                                                    |
| `instanceConfig`                   | [object](#instanceconfig)                   | No       | DB Instance Configuration                                                                      |
| `kmsKeyId`                         | string                                      | No       | KMS key to use when encryptionAtRest = true (optional; default RDS KMS key is used if omitted) |
| `port`                             | integer                                     | No       |                                                                                                |
| `preferredBackupWindow`            | string                                      | No       | UTC, hh24:mi-hh24:mi, 30-min granularity                                                       |
| `preferredMaintenanceWindow`       | string                                      | No       | UTC, hh24:mi-hh24:mi                                                                           |
| `readers`                          | [object](#readers)[]                        | No       | List of Reader DB instance specific configuration                                              |
| `replicationSourceIdentifier`      | string                                      | No       | Aurora replica source cluster ARN/ID                                                           |
| `serverlessV2ScalingConfiguration` | [object](#serverlessv2scalingconfiguration) | No       |                                                                                                |
| `snapshotIdentifier`               | string                                      | No       | Restore from snapshot                                                                          |
| `sourceRegion`                     | string                                      | No       | Source region (used to assist cross-Region replica creation)                                   |
| `storageType`                      | string                                      | No       | Storage type for rds cluster Possible values are: `aurora`, `aurora-iopt1`.                    |
| `tags`                             | [object](#tags)                             | No       | Tags for rds cluster as per AWS                                                                |

#### clusterParameterGroupConfig

Inline cluster parameter overrides (Aurora MySQL)

##### Properties

| Property                  | Type   | Required | Description                        |
|---------------------------|--------|----------|------------------------------------|
| `awsDefaultLambdaRole`    | string | No       |                                    |
| `awsDefaultLogsRole`      | string | No       |                                    |
| `awsDefaultS3Role`        | string | No       |                                    |
| `binlogFormat`            | string | No       | Possible values are: `ROW`, `OFF`. |
| `innodbPrintAllDeadlocks` | string | No       |                                    |

#### credentials

Choose static username/password OR Secrets Manager–managed

##### Properties

| Property                   | Type    | Required | Description |
|----------------------------|---------|----------|-------------|
| `masterUsername`           | string  | **Yes**  |             |
| `manageMasterUserPassword` | boolean | No       |             |
| `masterUserPassword`       | string  | No       |             |
| `masterUserSecretKmsKeyId` | string  | No       |             |

#### deletionConfig

Preferences used when deleting this cluster

##### Properties

| Property                  | Type    | Required | Description                                          |
|---------------------------|---------|----------|------------------------------------------------------|
| `finalSnapshotIdentifier` | string  | No       | Name for the final DB cluster snapshot when deleting |
| `skipFinalSnapshot`       | boolean | No       | If false, a final snapshot is required on delete     |

#### instanceConfig

DB Instance Configuration

##### Properties

| Property                             | Type                                    | Required | Description                               |
|--------------------------------------|-----------------------------------------|----------|-------------------------------------------|
| `instanceType`                       | string                                  | **Yes**  | e.g., db.r6g.large or db.serverless       |
| `autoMinorVersionUpgrade`            | boolean                                 | No       |                                           |
| `availabilityZone`                   | string                                  | No       |                                           |
| `deletionProtection`                 | boolean                                 | No       |                                           |
| `enablePerformanceInsights`          | boolean                                 | No       |                                           |
| `enhancedMonitoring`                 | [object](#enhancedmonitoring)           | No       |                                           |
| `instanceParameterGroupConfig`       | [object](#instanceparametergroupconfig) | No       | Inline instance parameters (Aurora MySQL) |
| `instanceParameterGroupName`         | string                                  | No       |                                           |
| `networkType`                        | string                                  | No       | Possible values are: `IPV4`, `DUAL`.      |
| `performanceInsightsKmsKeyId`        | string                                  | No       |                                           |
| `performanceInsightsRetentionPeriod` | integer                                 | No       | Possible values are: `7`, `731`.          |
| `publiclyAccessible`                 | boolean                                 | No       |                                           |

##### enhancedMonitoring

###### Properties

| Property            | Type    | Required | Description                                            |
|---------------------|---------|----------|--------------------------------------------------------|
| `enabled`           | boolean | No       |                                                        |
| `interval`          | integer | No       | Possible values are: `1`, `5`, `10`, `15`, `30`, `60`. |
| `monitoringRoleArn` | string  | No       |                                                        |

##### instanceParameterGroupConfig

Inline instance parameters (Aurora MySQL)

###### Properties

| Property             | Type    | Required | Description |
|----------------------|---------|----------|-------------|
| `interactiveTimeout` | integer | No       |             |
| `lockWaitTimeout`    | integer | No       |             |
| `longQueryTime`      | integer | No       |             |
| `maxAllowedPacket`   | integer | No       |             |
| `maxHeapTableSize`   | integer | No       |             |
| `slowQueryLog`       | integer | No       |             |
| `tmpTableSize`       | integer | No       |             |
| `waitTimeout`        | integer | No       |             |

#### readers

Reader DB instance specific configuration

##### Properties

| Property        | Type    | Required | Description                            |
|-----------------|---------|----------|----------------------------------------|
| `instanceCount` | integer | **Yes**  | Number of reader instances             |
| `instanceType`  | string  | **Yes**  | e.g., db.r6g.large or db.serverless    |
| `promotionTier` | integer | No       | Promotion tier for the reader instance |

#### serverlessV2ScalingConfiguration

##### Properties

| Property      | Type   | Required | Description |
|---------------|--------|----------|-------------|
| `maxCapacity` | number | **Yes**  |             |
| `minCapacity` | number | **Yes**  |             |

#### tags

Tags for rds cluster as per AWS

| Property | Type | Required | Description |
|----------|------|----------|-------------|

#### writer

Writer DB instance specific configuration

##### Properties

| Property        | Type    | Required | Description                            |
|-----------------|---------|----------|----------------------------------------|
| `instanceType`  | string  | **Yes**  | e.g., db.r6g.large or db.serverless    |
| `promotionTier` | integer | No       | Promotion tier for the writer instance |



### Running Mysql

* Create an Intellij Run configuration
* Pass operation name as command line argument
* Pass following environment variables
  * `COMPONENT_METADATA`: [componentMetadata.json](../example/stag_aws_rds/componentMetadata.json)
  * `CONFIG`: merged json of [base_config.json](../example/stag_aws_rds/base_config.json) and [flavour_config.json](../example/stag_aws_rds/flavour_config.json). In the case of operation [operation_config.json](../example/stag_aws_rds/operation_config.json)
