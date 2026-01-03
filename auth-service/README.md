# Auth Service

Authentication and user management service for the Hospital Management System, built with **Quarkus Reactive**.

## Features

- ✅ **JWT Authentication** - SmallRye JWT with RS256/HS256 signing
- ✅ **Multi-Tenancy** - Tenant isolation via `tenantId` discriminator
- ✅ **Dynamic RBAC** - Role-Based Access Control with runtime permission resolution
- ✅ **Account Security**
  - BCrypt password hashing
  - Failed login tracking
  - Account lockout after 5 failed attempts (30-minute lockout)
  - Password complexity validation
- ✅ **Reactive Architecture** - Mutiny Uni/Multi throughout
- ✅ **Service Discovery** - Auto-registration with Consul
- ✅ **Event Publishing** - RabbitMQ events (UserRegisteredEvent, UserUpdatedEvent)
- ✅ **Health & Metrics** - SmallRye Health, Micrometer Prometheus

## Architecture

```
Client Request
    ↓
[API Gateway] → JWT Validation, Rate Limiting
    ↓
[Auth Controller] → /api/auth/login, /api/auth/register
    ↓
[AuthService] → Business logic, JWT generation
    ↓
[UserRepository] → Reactive Panache (PostgreSQL)
    ↓
[RBAC Integration] → PermissionService, RoleRepository
```

## Endpoints

### Public Endpoints (No JWT Required)

| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| POST | `/api/auth/register` | Register new user | RegisterRequest | UserDto (201) |
| POST | `/api/auth/login` | User login | LoginRequest | LoginResponse (200) |
| GET | `/api/auth/health` | Health check | - | HealthResponse (200) |

### Login Request

```json
{
  "email": "doctor@hospital.com",
  "password": "SecurePass123!"
}
```

### Login Response

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "tenantId": "hospital-001",
    "name": "Dr. John Doe",
    "email": "doctor@hospital.com",
    "active": true,
    "roles": ["DOCTOR"],
    "permissions": ["patient:read", "appointment:create", "medical_record:write"]
  }
}
```

### Register Request

```json
{
  "name": "Dr. Jane Smith",
  "email": "jane@hospital.com",
  "password": "SecurePass123!",
  "phone": "+1234567890"
}
```

**Password Requirements:**
- Minimum 8 characters, maximum 72
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (@$!%*?&)

## JWT Token Claims

Generated JWT contains:

```json
{
  "sub": "user-uuid",
  "tenantId": "hospital-001",
  "email": "doctor@hospital.com",
  "name": "Dr. John Doe",
  "roles": ["DOCTOR"],
  "permissions": ["patient:read", "appointment:create"],
  "iss": "hospital-system",
  "iat": 1234567890,
  "exp": 1234654290
}
```

## Multi-Tenancy

**Tenant Context Resolution:**
1. **API Gateway**: Extracts tenantId from subdomain or JWT, adds `X-Tenant-Id` header
2. **Auth Service**: Uses `X-Tenant-Id` header or defaults to `default-tenant` for testing
3. **Database**: All queries filter by `tenantId` column

**Tenant Isolation:**
- User entity has `tenantId` field
- Email is unique per tenant (not globally)
- All queries in UserRepository filter by tenantId
- JWT includes tenantId claim

## RBAC Integration

**Role Assignment:**
- Roles stored in `user_roles` table (many-to-many)
- Permissions stored in `role_permissions` table
- JWT contains flattened `roles[]` and `permissions[]` arrays

**Permission Resolution:**
1. Get user's active roles from `user_roles`
2. For each role, fetch permissions from `role_permissions`
3. Aggregate into unique set of permission names
4. Include in JWT claims for fast authorization checks

**Default Roles:**
- ADMIN - Full system access
- DOCTOR - Patient records, appointments, prescriptions
- NURSE - Patient care, vitals, medications
- RECEPTIONIST - Appointments, patient registration
- PATIENT - Own medical records (read-only)

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

# JWT
JWT_ISSUER=hospital-system
JWT_EXPIRATION_SECONDS=86400

# Multi-Tenancy
TENANT_DEFAULT_TENANT_ID=default-tenant

# Account Security
AUTH_LOCKOUT_THRESHOLD=5
AUTH_LOCKOUT_DURATION_MINUTES=30
```

### application.properties

See `src/main/resources/application.properties` for full configuration.

## Running the Service

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL (via Docker Compose)
- Redis (via Docker Compose)
- Consul (via Docker Compose)
- RabbitMQ (via Docker Compose)

### Start Infrastructure

```bash
cd docker
docker-compose up -d postgres redis consul rabbitmq
```

Verify services:
```bash
# PostgreSQL
docker exec -it hospital-postgres psql -U postgres -c "SELECT version();"

# Redis
docker exec -it hospital-redis redis-cli ping

# Consul
curl http://localhost:8500/v1/status/leader

# RabbitMQ
curl -u admin:admin123 http://localhost:15672/api/overview
```

### Run Database Migrations

```bash
# From project root
cd hospital-common-quarkus
mvn flyway:migrate

# Or use Docker
docker run --rm --network=hospital-network \
  -v $(pwd)/hospital-common-quarkus/src/main/resources/db/migration:/flyway/sql \
  flyway/flyway:10 \
  -url=jdbc:postgresql://hospital-postgres:5432/hospital_db \
  -user=postgres \
  -password=postgres \
  migrate
```

Verify migrations:
```bash
docker exec -it hospital-postgres psql -U postgres -d hospital_db -c \
  "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

### Development Mode (Live Reload)

```bash
cd auth-service
./mvnw quarkus:dev
```

Service runs on: http://localhost:8081

**Dev UI:** http://localhost:8081/q/dev

**Features:**
- Live reload on code changes
- Continuous testing (press `r`)
- Database inspection
- Configuration editor

### Production (JVM)

```bash
# Build
./mvnw clean package

# Run
java -jar target/quarkus-app/quarkus-run.jar
```

### Production (Native)

```bash
# Build native binary
./mvnw package -Pnative

# Run
./target/auth-service-1.0.0-runner
```

**Native startup:** ~0.5-1s (vs ~3-5s JVM)
**Native memory:** ~50-80MB (vs ~150-200MB JVM)

## Testing

### Unit Tests

```bash
./mvnw test
```

### Integration Tests

```bash
./mvnw verify
```

### Manual Testing with cURL

**Register User:**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{
    "name": "Test User",
    "email": "test@hospital.com",
    "password": "SecurePass123!",
    "phone": "+1234567890"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{
    "email": "test@hospital.com",
    "password": "SecurePass123!"
  }' | jq
```

**Save token:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{"email":"test@hospital.com","password":"SecurePass123!"}' \
  | jq -r '.token')

echo $TOKEN
```

### Testing with API Gateway

Once API Gateway is running:

```bash
# Login via gateway (port 8080)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@hospital.com",
    "password": "SecurePass123!"
  }' | jq
```

## Monitoring

### Health Checks

- **Liveness:** `GET /q/health/live` - Service is alive
- **Readiness:** `GET /q/health/ready` - Service is ready to accept requests
- **Full Health:** `GET /q/health` - Detailed health status

```bash
curl http://localhost:8081/q/health | jq
```

### Metrics

Prometheus metrics available at `/q/metrics`:

```bash
curl http://localhost:8081/q/metrics | grep auth
```

**Key Metrics:**
- `http_server_requests_total` - Total requests
- `http_server_requests_duration_seconds` - Request latency
- `jvm_memory_used_bytes` - Memory usage

### Consul Registration

Verify service is registered:

```bash
# List all services
curl http://localhost:8500/v1/catalog/services

# Get auth-service instances
curl http://localhost:8500/v1/catalog/service/auth-service | jq

# Consul UI
open http://localhost:8500/ui/dc1/services/auth-service
```

## Security Features

### Password Security

- **Hashing:** BCrypt with cost factor 10
- **Validation:** Complex requirements enforced
- **Storage:** Only hash stored, never plaintext

### Account Lockout

- **Threshold:** 5 failed login attempts
- **Duration:** 30 minutes
- **Reset:** Automatic after successful login
- **Monitoring:** Track locked accounts via `findLockedUsers()`

### JWT Security

- **Signing:** RS256 (production) or HS256 (development)
- **Expiration:** 24 hours default
- **Claims:** Includes tenantId, roles, permissions
- **Validation:** Automatic via SmallRye JWT

### Tenant Isolation

- **Database:** Row-level filtering by tenantId
- **Events:** tenantId in all event payloads
- **Caching:** Tenant-prefixed cache keys
- **Defense in Depth:** PostgreSQL Row-Level Security enabled

## Event Publishing

### UserRegisteredEvent

Published when user registers successfully.

**Consumers:**
- Notification Service (send welcome email)
- Audit Service (log user creation)

**Payload:**
```json
{
  "eventId": "uuid",
  "occurredAt": "2024-01-01T12:00:00Z",
  "tenantId": "hospital-001",
  "userId": "uuid",
  "name": "Test User",
  "email": "test@hospital.com",
  "phone": "+1234567890"
}
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8081
lsof -i :8081

# Kill process
kill -9 <PID>

# Or use different port
QUARKUS_HTTP_PORT=8091 ./mvnw quarkus:dev
```

### Database Connection Failed

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs hospital-postgres

# Restart
docker-compose restart postgres

# Test connection
docker exec -it hospital-postgres psql -U postgres -c "SELECT 1;"
```

### Consul Registration Failed

```bash
# Check Consul is running
curl http://localhost:8500/v1/status/leader

# Check service logs
./mvnw quarkus:dev | grep -i consul

# Verify config
grep consul application.properties
```

### JWT Generation Failed

```bash
# Check JWT configuration
grep jwt application.properties

# Verify keys exist (for RS256)
ls -la src/main/resources/privateKey.pem
ls -la src/main/resources/publicKey.pem

# Generate keys if missing
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
```

## Next Steps

- [ ] Add email verification flow
- [ ] Add password reset functionality
- [ ] Add 2FA support (TOTP)
- [ ] Add OAuth2/OIDC integration
- [ ] Add user profile management endpoints
- [ ] Add role assignment endpoints (admin)
- [ ] Add audit logging for authentication events

---

**Service Port:** 8081
**Health:** http://localhost:8081/q/health
**Metrics:** http://localhost:8081/q/metrics
**Dev UI:** http://localhost:8081/q/dev
