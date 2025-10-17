## Update Cluster operation

Update cluster

### Update config operation schema

#### Properties

| Property                           | Type                                        | Required | Description                                                                 |
|------------------------------------|---------------------------------------------|----------|-----------------------------------------------------------------------------|
| `applyImmediately`                 | boolean                                     | **Yes**  | Apply changes immediately or during next maintenance window                 |
| `backtrackWindow`                  | integer                                     | No       | Aurora Backtrack window in seconds (if supported by engine version)         |
| `backupRetentionPeriod`            | integer                                     | No       |                                                                             |
| `clusterParameterGroupConfig`      | [object](#clusterparametergroupconfig)      | No       | Inline cluster parameter overrides (Aurora MySQL)                           |
| `clusterParameterGroupName`        | string                                      | No       | Use an existing cluster parameter group                                     |
| `copyTagsToSnapshot`               | boolean                                     | No       |                                                                             |
| `credentials`                      | [object](#credentials)                      | No       | Update credentials for the cluster                                          |
| `deletionConfig`                   | [object](#deletionconfig)                   | No       | Preferences used when deleting this cluster                                 |
| `deletionProtection`               | boolean                                     | No       |                                                                             |
| `enableIAMDatabaseAuthentication`  | boolean                                     | No       | Enable IAM DB authentication for the cluster                                |
| `engineVersion`                    | string                                      | No       | Aurora MySQL engine version, e.g. 3.04.2                                    |
| `instanceConfig`                   | [object](#instanceconfig)                   | No       | DB Instance Configuration for updates                                       |
| `port`                             | integer                                     | No       |                                                                             |
| `preferredBackupWindow`            | string                                      | No       | UTC, hh24:mi-hh24:mi, 30-min granularity                                    |
| `preferredMaintenanceWindow`       | string                                      | No       | UTC, ddd:hh24:mi-ddd:hh24:mi                                                |
| `serverlessV2ScalingConfiguration` | [object](#serverlessv2scalingconfiguration) | No       |                                                                             |
| `storageType`                      | string                                      | No       | Storage type for rds cluster Possible values are: `aurora`, `aurora-iopt1`. |
| `tags`                             | [object](#tags)                             | No       | Tags for rds cluster as per AWS                                             |

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

Update credentials for the cluster

##### Properties

| Property                   | Type    | Required | Description |
|----------------------------|---------|----------|-------------|
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

DB Instance Configuration for updates

##### Properties

| Property                             | Type                                    | Required | Description                               |
|--------------------------------------|-----------------------------------------|----------|-------------------------------------------|
| `autoMinorVersionUpgrade`            | boolean                                 | No       |                                           |
| `deletionProtection`                 | boolean                                 | No       |                                           |
| `enablePerformanceInsights`          | boolean                                 | No       |                                           |
| `enhancedMonitoring`                 | [object](#enhancedmonitoring)           | No       |                                           |
| `instanceParameterGroupConfig`       | [object](#instanceparametergroupconfig) | No       | Inline instance parameters (Aurora MySQL) |
| `instanceParameterGroupName`         | string                                  | No       |                                           |
| `performanceInsightsKmsKeyId`        | string                                  | No       |                                           |
| `performanceInsightsRetentionPeriod` | integer                                 | No       | Possible values are: `7`, `731`.          |

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


