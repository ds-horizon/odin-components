## Failover operation

Failover a reader instance

### Reboot operation schema

#### Properties

| Property              | Type     | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-----------------------|----------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `instanceIdentifiers` | string[] | **Yes**  | Identifiers of RDS database instances that will be rebooted to apply certain configuration changes or resolve issues. Rebooting is necessary when parameter changes require a restart, to clear database cache, or to resolve performance degradation. `Impact:` Causes temporary downtime (typically 1-5 minutes), drops all active database connections. `Production:` Always schedule reboots during maintenance windows, notify dependent services beforehand, ensure application retry logic is in place, and avoid rebooting multiple instances simultaneously in clustered environments. |


