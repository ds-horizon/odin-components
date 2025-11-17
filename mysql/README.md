# Mysql component

Deploy and perform operations on your mysql component across different flavors

## Flavors
- [aws_rds](aws_rds)

## Schema valid for across all flavours

### Properties

| Property    | Type                 | Required | Description                                                                                                                                                                                                                                                                                                                                                    |
|-------------|----------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `discovery` | [object](#discovery) | **Yes**  | Discovery config for the MySQL cluster                                                                                                                                                                                                                                                                                                                         |
| `version`   | string               | **Yes**  | MySQL version to be created Possible values are: `8.0`.                                                                                                                                                                                                                                                                                                        |
| `password`  | string               | No       | MySQL database password for authentication and access control. Secures database access and prevents unauthorized connections. Store securely in secrets management systems (e.g., Vault, AWS Secrets Manager). **Required:** Strong passwords recommended (16+ chars, mixed case, special chars). **Production:** Rotate regularly and use secrets management. |
| `username`  | string               | No       | MySQL database username for authentication and connection. Used by applications to connect and perform database operations. Changing this requires updating all client connections and service accounts. **Required:** Must match MySQL user privileges. **Production:** Use dedicated service accounts with least-privilege access.                           |

### discovery

Discovery config for the MySQL cluster

#### Properties

| Property | Type   | Required | Description                                   |
|----------|--------|----------|-----------------------------------------------|
| `reader` | string | **Yes**  | The private discovery endpoint for the reader |
| `writer` | string | **Yes**  | The private discovery endpoint for the writer |


