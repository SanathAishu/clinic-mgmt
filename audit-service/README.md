# Audit Service

Audit logging service for tracking all system events, changes, and user actions. Built with **Quarkus Reactive**.

## Features

- ✅ **Event-Driven Architecture** - Consumes events from RabbitMQ
- ✅ **Multi-Tenancy** - Tenant isolation via `tenantId` discriminator
- ✅ **Immutable Audit Trail** - Append-only (no updates/deletes)
- ✅ **DPDPA Compliance** - 7-year retention for healthcare data
- ✅ **Comprehensive Logging** - Tracks all CRUD operations, logins, access
- ✅ **RBAC Integration** - Admin-only access to audit logs
- ✅ **Reactive Architecture** - Mutiny Uni/Multi throughout
- ✅ **Service Discovery** - Auto-registration with Consul
- ✅ **Powerful Querying** - Search by user, resource, action, date range

## Architecture

```
RabbitMQ Events
    ↓
[UserEventListener] → @Incoming("user-registered-events")
    ↓
[AuditLog Entity] → PostgreSQL (audit_logs table)
    ↓
[AuditController] → Query audit logs (admin only)
```

## Event Sources

Audit service consumes events from:

| Event | Source Service | Routing Key | Description |
|-------|---------------|-------------|-------------|
| UserRegisteredEvent | auth-service | user.registered | User registration |
| UserUpdatedEvent | auth-service | user.updated | User profile updates |
| _Future events_ | _Other services_ | _Various_ | Patient, Doctor, Appointment changes |

## Endpoints

### Audit Log Queries (Admin Only)

All endpoints require `ADMIN` or `AUDITOR` role.

| Method | Path | Description | Query Params |
|--------|------|-------------|--------------|
| GET | `/api/audit/logs` | Get all audit logs | page, size |
| GET | `/api/audit/logs/user/{userId}` | Get user activity | page, size |
| GET | `/api/audit/logs/{resourceType}/{resourceId}` | Get resource history | - |
| GET | `/api/audit/logs/action/{action}` | Get logs by action | page, size |
| GET | `/api/audit/logs/daterange` | Get logs in date range | startDate, endDate, page, size |
| GET | `/api/audit/logs/recent` | Get recent logs | limit |
| GET | `/api/audit/logs/failed` | Get failed operations | page, size |
| GET | `/api/audit/logs/search` | Search logs | q, page, size |
| GET | `/api/audit/statistics` | Get audit statistics | - |
| GET | `/api/audit/health` | Health check | - |

### Example Requests

**Get Recent Audit Logs:**
```bash
TOKEN="your-jwt-token"

curl -X GET http://localhost:8088/api/audit/logs/recent?limit=10 \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Response:**
```json
[
  {
    "id": "uuid",
    "tenantId": "test-tenant",
    "userId": "uuid",
    "userEmail": "test@hospital.com",
    "action": "REGISTER",
    "resourceType": "USER",
    "resourceId": "uuid",
    "description": "User registered: Test User (test@hospital.com)",
    "newValue": "{\"name\":\"Test User\",\"email\":\"test@hospital.com\"}",
    "eventId": "uuid",
    "timestamp": "2024-01-01T12:00:00"
  }
]
```

**Get User Activity:**
```bash
curl -X GET http://localhost:8088/api/audit/logs/user/{userId}?page=0&size=20 \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Get Resource History:**
```bash
curl -X GET http://localhost:8088/api/audit/logs/PATIENT/uuid \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Search Audit Logs:**
```bash
curl -X GET "http://localhost:8088/api/audit/logs/search?q=registered&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Get Audit Statistics:**
```bash
curl -X GET http://localhost:8088/api/audit/statistics \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Response:**
```json
{
  "total": 150,
  "creates": 45,
  "updates": 80,
  "deletes": 5,
  "failed": 20
}
```

## Multi-Tenancy

**Tenant Isolation:**
- All audit logs have `tenantId` column
- All queries filter by JWT's `tenantId` claim
- PostgreSQL Row-Level Security enforces isolation
- Audit logs cannot cross tenant boundaries

**How It Works:**
1. Event arrives with `tenantId` from source service
2. Audit log created with that `tenantId`
3. Query endpoints extract `tenantId` from JWT
4. Repository filters all queries by `tenantId`
5. RLS provides defense-in-depth

## RBAC Integration

**Authorization:**
- All query endpoints require `audit:read` permission
- Enforced via `PermissionService.requirePermission()`
- Only ADMIN and AUDITOR roles have access
- Permission checks happen in service layer

**Role Requirements:**
```java
@RolesAllowed({"ADMIN", "AUDITOR"})
public Uni<Response> getAuditLogs() {
    // Only admins and auditors can access
}
```

## Database Schema

### audit_logs Table

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    user_id UUID,
    user_email VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    description VARCHAR(500),
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    event_id UUID,
    http_method VARCHAR(10),
    request_path VARCHAR(500),
    status_code INTEGER,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Indexes:**
- `(tenant_id, timestamp DESC)` - Main query index
- `(user_id, timestamp DESC)` - User activity
- `(resource_type, resource_id, timestamp DESC)` - Resource history
- `(action, timestamp DESC)` - Action filtering
- `(event_id)` - Event correlation

**Row-Level Security:**
```sql
-- Users can only see logs for their tenant
CREATE POLICY audit_logs_tenant_isolation ON audit_logs
    FOR SELECT
    USING (tenant_id = current_setting('app.current_tenant_id', true));

-- Only INSERT allowed (immutable audit trail)
CREATE POLICY audit_logs_insert_only ON audit_logs
    FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));
```

## Event Processing

### UserEventListener

Consumes user events from auth-service:

```java
@Incoming("user-registered-events")
public Uni<Void> handleUserRegistered(UserRegisteredEvent event) {
    // Validate tenantId
    if (event.getTenantId() == null) {
        return Uni.createFrom().voidItem();
    }

    // Create audit log
    AuditLog auditLog = new AuditLog();
    auditLog.setTenantId(event.getTenantId());
    auditLog.setUserId(event.getUserId());
    auditLog.setAction("REGISTER");
    auditLog.setResourceType("USER");
    // ... set other fields

    // Persist
    return auditLogRepository.persist(auditLog).replaceWithVoid();
}
```

**RabbitMQ Configuration:**
```properties
mp.messaging.incoming.user-registered-events.connector=smallrye-rabbitmq
mp.messaging.incoming.user-registered-events.queue.name=audit.user.registered
mp.messaging.incoming.user-registered-events.exchange.name=hospital.events.direct
mp.messaging.incoming.user-registered-events.routing-keys=user.registered
```

## Configuration

### Environment Variables

```bash
# Database
QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://localhost:5432/hospital_db
QUARKUS_DATASOURCE_USERNAME=postgres
QUARKUS_DATASOURCE_PASSWORD=postgres

# Consul
QUARKUS_CONSUL_CONFIG_ENABLED=true
QUARKUS_CONSUL_CONFIG_AGENT_HOST_PORT=localhost:8500

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin123

# JWT
JWT_ISSUER=hospital-system
```

### application.properties

See `src/main/resources/application.properties` for full configuration.

## Running the Service

### Prerequisites

- Java 21+
- Gradle (wrapper included in project)
- PostgreSQL (via Docker Compose)
- RabbitMQ (via Docker Compose)
- Consul (via Docker Compose)
- Redis (via Docker Compose)

### Start Infrastructure

```bash
cd docker
docker-compose up -d postgres redis consul rabbitmq
```

### Run Database Migrations

```bash
# Run V007 migration (creates audit_logs table)
cd hospital-common-lib
../gradlew flywayMigrate
```

Verify:
```bash
docker exec -it hospital-postgres psql -U postgres -d hospital_db -c \
  "SELECT installed_rank, description FROM flyway_schema_history WHERE installed_rank = 7;"
```

### Development Mode

```bash
# From project root
./gradlew :audit-service:quarkusDev
```

Service runs on: http://localhost:8088

**Dev UI:** http://localhost:8088/q/dev

### Production (JVM)

```bash
./gradlew :audit-service:build -x test
java -jar audit-service/build/quarkus-app/quarkus-run.jar
```

### Production (Native)

```bash
./gradlew :audit-service:build -Dquarkus.package.type=native -x test
./audit-service/build/audit-service-1.0.0-runner
```

## Testing End-to-End Event Flow

### Step 1: Start Services

```bash
# Terminal 1: Auth Service (from project root)
./gradlew :auth-service:quarkusDev

# Terminal 2: Audit Service (from project root)
./gradlew :audit-service:quarkusDev
```

### Step 2: Register a User (Triggers Event)

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{
    "name": "Test User",
    "email": "audit-test@hospital.com",
    "password": "SecurePass123!",
    "phone": "+1234567890"
  }' | jq
```

**Expected:**
- Auth service logs: "Publishing UserRegisteredEvent..."
- RabbitMQ queue created: `audit.user.registered`
- Audit service logs: "Processing UserRegisteredEvent..."
- Audit log entry created in database

### Step 3: Verify RabbitMQ

```bash
# Check exchange
curl -u admin:admin123 http://localhost:15672/api/exchanges/hospital/hospital.events.direct | jq

# Check queue
curl -u admin:admin123 http://localhost:15672/api/queues/hospital/audit.user.registered | jq

# Should show message delivered
```

### Step 4: Query Audit Logs

```bash
# Login to get JWT token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{"email":"audit-test@hospital.com","password":"SecurePass123!"}' \
  | jq -r '.token')

# Get recent audit logs (requires ADMIN role)
curl -X GET http://localhost:8088/api/audit/logs/recent?limit=5 \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Expected Response:**
```json
[
  {
    "action": "REGISTER",
    "resourceType": "USER",
    "description": "User registered: Test User (audit-test@hospital.com)",
    "timestamp": "2024-01-01T12:00:00"
  }
]
```

## Monitoring

### Health Checks

```bash
curl http://localhost:8088/q/health | jq
```

### Metrics

```bash
curl http://localhost:8088/q/metrics | grep audit
```

### Consul Registration

```bash
# Verify service registered
curl http://localhost:8500/v1/catalog/service/audit-service | jq

# Consul UI
open http://localhost:8500/ui/dc1/services/audit-service
```

### RabbitMQ Management

```bash
# Check queues
curl -u admin:admin123 http://localhost:15672/api/queues | jq '.[] | select(.name | contains("audit"))'

# RabbitMQ UI
open http://localhost:15672
```

## DPDPA Compliance

### 7-Year Retention

Audit logs must be retained for minimum 7 years for healthcare data.

**Retention Function:**
```sql
SELECT archive_old_audit_logs(7);  -- Returns count of logs > 7 years old
```

In production, old logs should be archived (not deleted):
- Move to cold storage (S3, etc.)
- Maintain in compressed format
- Keep accessible for compliance audits

### Immutable Audit Trail

- **No UPDATE operations** - audit logs are never modified
- **No DELETE operations** - audit logs are never deleted (only archived)
- **INSERT only** - append-only audit trail
- **RLS enforced** - PostgreSQL policies prevent updates/deletes

### Data Breach Tracking

Related table: `data_breach_logs` (from V006 migration)
- Tracks data breach incidents
- Links to audit logs via timestamps
- Required for DPDPA breach notification (72 hours)

## Troubleshooting

### Common Issues

#### 1. JSON Deserialization Error (ClassCastException)

**Symptom:**
```
ClassCastException: io.vertx.core.json.JsonObject cannot be cast to UserRegisteredEvent
```

**Solution:**
Event listeners must accept `JsonObject` and manually deserialize. See [TROUBLESHOOTING.md](../TROUBLESHOOTING.md#issue-json-deserialization---classcastexception) for details.

#### 2. Events Processed But No Database Rows

**Symptom:**
- Logs show "Processing UserRegisteredEvent..."
- No errors
- Database table empty

**Solution:**
Use `@WithTransaction` instead of `@WithSession` for event listeners that perform database writes. See [TROUBLESHOOTING.md](../TROUBLESHOOTING.md#issue-event-processing-succeeds-but-no-database-rows-created) for details.

#### 3. Events Not Being Received

```bash
# Check RabbitMQ queues
curl -u admin:admin123 http://localhost:15672/api/queues/hospital | jq

# Check audit service logs
./gradlew quarkusDev | grep "Processing.*Event"

# Verify auth service is publishing
# (register a user and check auth service logs for "Publishing UserRegisteredEvent")
```

#### 4. Permission Denied Errors

```bash
# Verify user has ADMIN or AUDITOR role
# Decode JWT token
echo $TOKEN | cut -d. -f2 | base64 -d | jq '.roles'

# Should include "ADMIN" or "AUDITOR"
```

#### 5. Database Connection Failed

```bash
# Check PostgreSQL
docker ps | grep postgres
docker logs hospital-postgres

# Verify migrations
docker exec -it hospital-postgres psql -U postgres -d hospital_db -c \
  "SELECT * FROM flyway_schema_history WHERE installed_rank = 7;"
```

### More Help

For comprehensive troubleshooting, see [TROUBLESHOOTING.md](../TROUBLESHOOTING.md) in the root directory.

## Next Steps

- [ ] Add HTTP request/response auditing filter
- [ ] Add audit log export (CSV, JSON, PDF)
- [ ] Add audit log retention automation
- [ ] Add more event listeners (Patient, Doctor, Appointment events)
- [ ] Add audit analytics dashboard
- [ ] Add compliance reporting

---

**Service Port:** 8088
**Health:** http://localhost:8088/q/health
**Metrics:** http://localhost:8088/q/metrics
**Dev UI:** http://localhost:8088/q/dev
