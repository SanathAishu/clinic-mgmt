# Troubleshooting Guide

Common issues and solutions for the Hospital Management System (Quarkus Reactive).

## Table of Contents

- [RabbitMQ Event Consumption](#rabbitmq-event-consumption)
- [Database Issues](#database-issues)
- [Native Build Problems](#native-build-problems)
- [Authentication & Authorization](#authentication--authorization)
- [Performance Issues](#performance-issues)
- [Docker & Infrastructure](#docker--infrastructure)

---

## RabbitMQ Event Consumption

### Issue: JSON Deserialization - ClassCastException

**Symptom:**
```
java.lang.ClassCastException: class io.vertx.core.json.JsonObject cannot be cast to class com.hospital.common.event.AuthEvents$UserRegisteredEvent
```

**Root Cause:**
SmallRye RabbitMQ's default deserializer returns `io.vertx.core.json.JsonObject` instead of the typed event class.

**Solution:**

Change event listener methods to accept `JsonObject` and manually deserialize:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class UserEventListener {

    @Inject
    ObjectMapper objectMapper;

    @Incoming("user-registered-events")
    @WithTransaction  // Important: use @WithTransaction for writes!
    public Uni<Void> handleUserRegistered(JsonObject jsonPayload) {
        // Manual deserialization
        UserRegisteredEvent event;
        try {
            event = objectMapper.readValue(jsonPayload.encode(), UserRegisteredEvent.class);
        } catch (Exception e) {
            Log.errorf(e, "Failed to deserialize UserRegisteredEvent: %s", jsonPayload);
            return Uni.createFrom().voidItem();
        }

        // Validate required fields
        if (event.getTenantId() == null || event.getTenantId().isBlank()) {
            Log.errorf("UserRegisteredEvent missing tenantId: eventId=%s", event.getEventId());
            return Uni.createFrom().voidItem();
        }

        // Process event...
        return processEvent(event);
    }
}
```

**Files Fixed:**
- `audit-service/src/main/java/com/hospital/audit/listener/UserEventListener.java`

---

### Issue: Event Processing Succeeds but No Database Rows Created

**Symptom:**
- Event listener logs show "Processing UserRegisteredEvent..."
- No errors in logs
- RabbitMQ queue shows messages consumed
- Database table remains empty (0 rows)

**Root Cause:**
Using `@WithSession` instead of `@WithTransaction`. `@WithSession` opens a Hibernate Reactive session but **does not commit the transaction**. Database writes require explicit transaction boundaries.

**Solution:**

Use `@WithTransaction` for event listeners that perform database writes:

```java
// ❌ WRONG - Session without transaction commit
@Incoming("user-registered-events")
@WithSession
public Uni<Void> handleUserRegistered(JsonObject jsonPayload) {
    return auditLogRepository.persist(auditLog).replaceWithVoid();
    // Transaction never commits!
}

// ✅ CORRECT - Transaction with automatic commit
@Incoming("user-registered-events")
@WithTransaction
public Uni<Void> handleUserRegistered(JsonObject jsonPayload) {
    return auditLogRepository.persist(auditLog).replaceWithVoid();
    // Transaction commits when Uni completes successfully
}
```

**Key Differences:**

| Annotation | Session | Transaction | Auto-Commit | Use Case |
|------------|---------|-------------|-------------|----------|
| `@WithSession` | ✅ | ❌ | ❌ | Read-only queries |
| `@WithTransaction` | ✅ | ✅ | ✅ | Database writes (INSERT/UPDATE/DELETE) |

**When to Use Each:**
- **@WithSession**: Read-only operations (queries, lookups)
- **@WithTransaction**: Any database write operation (persist, update, delete)

---

### Issue: Events Not Being Received

**Symptom:**
Event listener never invoked, no logs showing event processing.

**Diagnosis:**

```bash
# 1. Check RabbitMQ queues
docker exec hospital-rabbitmq rabbitmqctl list_queues name messages consumers -p hospital

# 2. Expected output
audit.user.registered    0    1    # 0 messages, 1 consumer (good)
audit.user.registered    5    0    # 5 messages, 0 consumers (BAD - no listener connected)
```

**Possible Causes:**

1. **RabbitMQ not running:**
   ```bash
   docker ps | grep rabbitmq
   docker-compose up -d rabbitmq
   ```

2. **Wrong queue/exchange configuration:**
   ```properties
   # Verify in application.properties
   mp.messaging.incoming.user-registered-events.connector=smallrye-rabbitmq
   mp.messaging.incoming.user-registered-events.queue.name=audit.user.registered
   mp.messaging.incoming.user-registered-events.exchange.name=hospital.events.direct
   mp.messaging.incoming.user-registered-events.routing-keys=user.registered
   ```

3. **Listener method not annotated:**
   ```java
   @Incoming("user-registered-events")  // Must match channel name
   public Uni<Void> handleUserRegistered(JsonObject event) { ... }
   ```

4. **Service not started:**
   ```bash
   ./gradlew :audit-service:quarkusDev
   ```

---

### Issue: RabbitMQ Connection Refused

**Symptom:**
```
Connection refused: localhost/127.0.0.1:5672
```

**Solution:**

```bash
# Check RabbitMQ container status
docker ps | grep rabbitmq

# If not running, start it
docker-compose up -d rabbitmq

# Verify connectivity
curl http://localhost:15672  # RabbitMQ Management UI

# Check credentials in application.properties
rabbitmq-username=admin
rabbitmq-password=admin123
rabbitmq-virtual-host=hospital
```

---

## Database Issues

### Issue: Row Level Security (RLS) Blocking Inserts

**Symptom:**
- Hibernate persist completes without errors
- No rows created in database
- PostgreSQL logs show RLS policy violations (if logging enabled)

**Root Cause:**
RLS policies require `app.current_tenant_id` session variable to be set before INSERT.

**Solution:**

**Option 1: Disable RLS (Development Only)**
```sql
ALTER TABLE audit_logs DISABLE ROW LEVEL SECURITY;
```

**Option 2: Set Session Variable (Production)**
```java
@WithTransaction
public Uni<Void> handleEvent(Event event) {
    String tenantId = event.getTenantId();

    // Set PostgreSQL session variable for RLS
    return sessionFactory.withSession(session -> {
        return session.createNativeQuery("SET LOCAL app.current_tenant_id = :tenantId")
            .setParameter("tenantId", tenantId)
            .executeUpdate()
            .chain(() -> auditLogRepository.persist(auditLog).replaceWithVoid());
    });
}
```

**Option 3: Exempt Audit Service from RLS**
```sql
-- Create dedicated audit service role
CREATE ROLE audit_service;

-- Grant BYPASSRLS privilege
ALTER TABLE audit_logs OWNER TO audit_service;
GRANT BYPASSRLS ON audit_logs TO audit_service;

-- Update datasource credentials
quarkus.datasource.username=audit_service
quarkus.datasource.password=secure_password
```

**Current Status:**
RLS is **disabled** for audit_logs table in development to simplify event processing. For production, implement Option 3 (dedicated audit service role with BYPASSRLS).

---

### Issue: No Current Mutiny.Session Found

**Symptom:**
```
java.lang.IllegalStateException: No current Mutiny.Session found
- no reactive session was found in the Vert.x context
- you may need to annotate the business method with @WithSession or @WithTransaction
```

**Solution:**

Add `@WithTransaction` (for writes) or `@WithSession` (for reads) to event listener methods:

```java
@Incoming("user-registered-events")
@WithTransaction  // ← Add this annotation
public Uni<Void> handleUserRegistered(JsonObject event) {
    return auditLogRepository.persist(auditLog).replaceWithVoid();
}
```

---

### Issue: Database Schema Not Created

**Symptom:**
```
ERROR: relation "audit_logs" does not exist
```

**Solution:**

```bash
# Run Flyway migrations
cd hospital-common-lib
./gradlew flywayMigrate

# Verify migration
docker exec hospital-postgres psql -U postgres -d hospital_db -c \
  "SELECT installed_rank, description FROM flyway_schema_history;"

# Expected output should include:
# 7 | Create audit_logs table with indexes
```

---

## Native Build Problems

### Issue: Reflection Registration Missing

**Symptom:**
```
ClassNotFoundException at runtime in native build
```

**Solution:**

Add `@RegisterForReflection` to all event classes, DTOs, and entities:

```java
@RegisterForReflection
public class UserRegisteredEvent {
    private String tenantId;
    private UUID userId;
    private String name;
    private String email;
    // ...
}
```

Or configure in `application.properties`:
```properties
quarkus.native.additional-build-args=--initialize-at-run-time=org.postgresql.Driver
quarkus.native.enable-all-security-services=true
```

---

### Issue: Native Build Fails with LinkageError

**Symptom:**
```
Error: LinkageError occurred while loading main class
```

**Solution:**

Increase native build memory:
```bash
./mvnw package -Pnative \
  -Dquarkus.native.native-image-xmx=4g \
  -DskipTests
```

---

## Authentication & Authorization

### Issue: JWT Token Invalid

**Symptom:**
```
401 Unauthorized: Invalid JWT token
```

**Diagnosis:**

```bash
# Decode JWT token
echo $TOKEN | cut -d. -f2 | base64 -d | jq

# Verify claims
{
  "sub": "user-id",
  "tenantId": "test-tenant",
  "roles": ["USER"],
  "permissions": ["patient:read"],
  "iss": "hospital-system",
  "exp": 1234567890
}
```

**Common Causes:**

1. **Expired token** - Check `exp` claim
2. **Wrong issuer** - Verify `iss` matches `jwt.issuer` in application.properties
3. **Missing public key** - Ensure `publicKey.pem` exists in `src/main/resources/`

---

### Issue: Permission Denied

**Symptom:**
```
403 Forbidden: Insufficient permissions
```

**Solution:**

```bash
# Verify user has required role
echo $TOKEN | cut -d. -f2 | base64 -d | jq '.roles'

# Audit endpoints require ADMIN or AUDITOR role
curl -X GET http://localhost:8088/api/audit/logs/recent \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Performance Issues

### Issue: Slow Event Processing

**Diagnosis:**

```bash
# Check RabbitMQ queue backlog
docker exec hospital-rabbitmq rabbitmqctl list_queues name messages -p hospital

# Check database query performance
docker exec hospital-postgres psql -U postgres -d hospital_db -c \
  "SELECT * FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;"
```

**Solutions:**

1. **Add indexes** to frequently queried columns
2. **Increase connection pool** size:
   ```properties
   quarkus.datasource.reactive.max-size=20  # Increase from default 10
   ```
3. **Enable reactive caching**:
   ```java
   @CacheResult(cacheName = "audit-logs")
   public Uni<List<AuditLogDto>> getRecentLogs(String tenantId) { ... }
   ```

---

## Docker & Infrastructure

### Issue: Docker Containers Not Starting

**Diagnosis:**

```bash
# Check container logs
docker logs hospital-postgres
docker logs hospital-rabbitmq
docker logs hospital-redis
docker logs hospital-consul

# Check container status
docker ps -a
```

**Common Causes:**

1. **Port conflicts:**
   ```bash
   # Check if ports are already in use
   lsof -i :5432  # PostgreSQL
   lsof -i :5672  # RabbitMQ
   lsof -i :6379  # Redis
   lsof -i :8500  # Consul
   ```

2. **Volume permission issues:**
   ```bash
   # Remove volumes and recreate
   docker-compose down -v
   docker-compose up -d
   ```

---

### Issue: Out of Disk Space

**Solution:**

```bash
# Clean up Docker resources
docker system prune -a --volumes

# Remove build artifacts
./gradlew clean

# Remove temporary files
rm -rf /tmp/audit-service*.log
rm -rf build/
rm -rf .gradle/
```

---

## Quick Diagnostic Checklist

When troubleshooting, follow this checklist:

- [ ] **Infrastructure running?** `docker ps` shows all 4 containers (postgres, rabbitmq, redis, consul)
- [ ] **Database migrated?** `flyway_schema_history` table exists with all migrations
- [ ] **Service started?** `./gradlew :service-name:quarkusDev` shows "Listening on..."
- [ ] **RabbitMQ connected?** Service logs show "Connection with RabbitMQ broker established"
- [ ] **Events published?** Auth service logs show "Publishing UserRegisteredEvent"
- [ ] **Events consumed?** Audit service logs show "Processing UserRegisteredEvent"
- [ ] **Database writes?** `SELECT COUNT(*) FROM audit_logs;` returns > 0
- [ ] **JWT valid?** Token decodes successfully and has correct claims

---

## Getting Help

If you encounter an issue not covered here:

1. **Check service logs:** `./gradlew :service-name:quarkusDev | grep ERROR`
2. **Check RabbitMQ Management UI:** http://localhost:15672 (admin/admin123)
3. **Check Consul UI:** http://localhost:8500/ui
4. **Check database:** `docker exec hospital-postgres psql -U postgres -d hospital_db`
5. **Search Quarkus guides:** https://quarkus.io/guides/

---

**Last Updated:** 2026-01-03
**Applies to:** Quarkus 3.15.1+
