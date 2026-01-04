# R2DBC Configuration Reference

## ✅ Completed Configuration

All 8 services have been configured with R2DBC PostgreSQL drivers.

### Services & Database Mappings

| Service | Port | Database Name | Config File |
|---------|------|---------------|-------------|
| auth-service | 8081 | auth_service | auth-service/src/main/resources/application.yml |
| patient-service | 8082 | patient_service | patient-service/src/main/resources/application.yml |
| doctor-service | 8083 | doctor_service | doctor-service/src/main/resources/application.yml |
| appointment-service | 8084 | appointment_service | appointment-service/src/main/resources/application.yml |
| medical-records-service | 8085 | medical_records_service | medical-records-service/src/main/resources/application.yml |
| facility-service | 8086 | facility_service | facility-service/src/main/resources/application.yml |
| notification-service | 8087 | notification_service | notification-service/src/main/resources/application.yml |
| audit-service | 8088 | audit_service | audit-service/src/main/resources/application.yml |

## R2DBC Configuration Details

### Connection URL Format
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/{database_name}
    username: postgres
    password: postgres
```

### Connection Pool Settings
```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10          # Initial connections in pool
      max-size: 20              # Maximum connections in pool
      max-acquire-time: 2s      # Max time to acquire connection
      max-create-connection-time: 2s  # Max time to create new connection
      validation-query: SELECT 1      # Query to validate connections
```

### Logging Configuration
```yaml
logging:
  level:
    com.hospital.{service}: DEBUG
    io.r2dbc.postgresql.QUERY: DEBUG  # Log SQL queries
    io.r2dbc.postgresql.PARAM: DEBUG  # Log query parameters
```

## Environment Variables Support

You can override the default configuration using environment variables:

```bash
# R2DBC Configuration
export R2DBC_URL="r2dbc:postgresql://prod-db:5432/patient_service"
export R2DBC_USERNAME="app_user"
export R2DBC_PASSWORD="secure_password"

# Connection Pool
export R2DBC_POOL_INITIAL_SIZE=20
export R2DBC_POOL_MAX_SIZE=50
export R2DBC_POOL_MAX_ACQUIRE_TIME=5s
```

Update application.yml to use these:
```yaml
spring:
  r2dbc:
    url: ${R2DBC_URL:r2dbc:postgresql://localhost:5432/patient_service}
    username: ${R2DBC_USERNAME:postgres}
    password: ${R2DBC_PASSWORD:postgres}
    pool:
      initial-size: ${R2DBC_POOL_INITIAL_SIZE:10}
      max-size: ${R2DBC_POOL_MAX_SIZE:20}
```

## Database Setup Required

### 1. Create Databases

Each service needs its own database:

```bash
# Connect to PostgreSQL
psql -U postgres

# Create databases
CREATE DATABASE auth_service;
CREATE DATABASE patient_service;
CREATE DATABASE doctor_service;
CREATE DATABASE appointment_service;
CREATE DATABASE medical_records_service;
CREATE DATABASE facility_service;
CREATE DATABASE notification_service;
CREATE DATABASE audit_service;

# Grant permissions (if using separate user)
GRANT ALL PRIVILEGES ON DATABASE patient_service TO app_user;
# ... repeat for other databases
```

### 2. Database Schema

Since we removed Hibernate's `ddl-auto`, you need to create tables manually or use migration tools.

**Option A: Use Flyway**

Add to each service's pom.xml:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Add to application.yml:
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

Create migration files in `src/main/resources/db/migration/`:
- `V1__Initial_schema.sql`
- `V2__Add_indexes.sql`

**Option B: Manual Schema Creation**

Example for patient_service:
```sql
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    gender VARCHAR(10),
    date_of_birth DATE,
    address VARCHAR(500),
    disease VARCHAR(50),
    medical_history TEXT,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

CREATE INDEX idx_patients_user_id ON patients(user_id);
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_disease ON patients(disease);
```

## Testing R2DBC Connection

Create a simple test controller to verify the connection:

```java
@RestController
@RequestMapping("/test")
public class R2dbcTestController {

    private final DatabaseClient databaseClient;

    public R2dbcTestController(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @GetMapping("/db-connection")
    public Mono<String> testConnection() {
        return databaseClient.sql("SELECT 1")
            .fetch()
            .one()
            .map(result -> "✓ R2DBC Connection Successful!")
            .onErrorReturn("✗ R2DBC Connection Failed");
    }
}
```

Test with:
```bash
curl http://localhost:8082/test/db-connection
```

## Connection String Examples

### Development
```
r2dbc:postgresql://localhost:5432/patient_service
```

### Production with SSL
```
r2dbc:postgresql://prod-db.example.com:5432/patient_service?sslMode=require
```

### With Custom Schema
```
r2dbc:postgresql://localhost:5432/patient_service?currentSchema=hospital
```

### Multiple Hosts (Failover)
```
r2dbc:postgresql://host1:5432,host2:5432/patient_service
```

## Connection Pool Tuning

### Low Traffic (Development)
```yaml
pool:
  initial-size: 5
  max-size: 10
```

### Medium Traffic (Staging)
```yaml
pool:
  initial-size: 10
  max-size: 20
```

### High Traffic (Production)
```yaml
pool:
  initial-size: 20
  max-size: 50
  max-acquire-time: 5s
  max-create-connection-time: 5s
```

## Troubleshooting

### Connection Refused
```
Error: Connection refused: localhost/127.0.0.1:5432
```
**Fix:** Ensure PostgreSQL is running:
```bash
sudo systemctl status postgresql
sudo systemctl start postgresql
```

### Authentication Failed
```
Error: password authentication failed for user "postgres"
```
**Fix:** Check credentials in application.yml or reset PostgreSQL password

### Database Does Not Exist
```
Error: database "patient_service" does not exist
```
**Fix:** Create the database:
```sql
CREATE DATABASE patient_service;
```

### Too Many Connections
```
Error: FATAL: remaining connection slots are reserved
```
**Fix:** Reduce pool max-size or increase PostgreSQL max_connections:
```sql
ALTER SYSTEM SET max_connections = 200;
SELECT pg_reload_conf();
```

## Migration from JPA to R2DBC Checklist

- [x] Update pom.xml dependencies (JPA → R2DBC)
- [x] Convert entities (@Entity → @Table)
- [x] Convert repositories (JpaRepository → R2dbcRepository)
- [x] Update application.yml (datasource → r2dbc)
- [ ] Create database schemas (Flyway or manual)
- [ ] Update service layer (handle Mono/Flux)
- [ ] Update controllers (reactive or blocking with .block())
- [ ] Update tests (use reactive test utilities)
- [ ] Enable R2DBC auditing (@EnableR2dbcAuditing)
- [ ] Test all endpoints with new configuration

## Next Steps

1. **Create Database Schemas**: Use Flyway or create tables manually
2. **Enable R2DBC Auditing**: Add `@EnableR2dbcAuditing` to main application class
3. **Update Service Layer**: Modify services to work with Mono/Flux
4. **Test Connections**: Verify all services can connect to their databases
5. **Performance Tuning**: Adjust pool sizes based on load testing
