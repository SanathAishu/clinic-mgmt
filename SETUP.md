# Hospital Management System - Setup Guide

Complete setup guide for the Quarkus Reactive microservices architecture.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Infrastructure Setup](#infrastructure-setup)
4. [Database Setup](#database-setup)
5. [Service Configuration](#service-configuration)
6. [Running Services](#running-services)
7. [Verification](#verification)
8. [Development Workflow](#development-workflow)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java** | 21+ | Runtime for Quarkus |
| **Gradle** | 8.x (wrapper included) | Build tool |
| **Docker** | 24+ | Infrastructure services |
| **Docker Compose** | 2.20+ | Orchestrate containers |
| **Git** | 2.40+ | Version control |

### Verify Installation

```bash
# Check Java
java -version  # Should show Java 21 or higher

# Check Gradle (wrapper included in project)
./gradlew --version

# Check Docker
docker --version
docker-compose --version

# Check Git
git --version
```

---

## Quick Start

**Get up and running in 5 minutes:**

```bash
# 1. Clone repository
git clone <repository-url>
cd Clinic-Mgmt-Quarkus

# 2. Start infrastructure (PostgreSQL, Redis, Consul, RabbitMQ)
cd docker
docker-compose up -d
cd ..

# 3. Build project
./gradlew clean build -x test

# 4. Run API Gateway (from project root)
./gradlew :api-gateway:quarkusDev

# 5. Access services
# Gateway: http://localhost:8080
# Consul UI: http://localhost:8500
# Grafana: http://localhost:3000
```

---

## Infrastructure Setup

### Start All Infrastructure Services

```bash
cd docker
docker-compose up -d
```

This starts:

| Service | Port | Purpose | UI/Access |
|---------|------|---------|-----------|
| **PostgreSQL** | 5432 | Database | - |
| **Redis** | 6379 | Cache & Rate Limiting | - |
| **Consul** | 8500 | Service Discovery | http://localhost:8500 |
| **RabbitMQ** | 5672, 15672 | Message Broker | http://localhost:15672 (admin/admin123) |
| **Prometheus** | 9090 | Metrics Collection | http://localhost:9090 |
| **Grafana** | 3000 | Metrics Visualization | http://localhost:3000 (admin/admin) |

### Verify Infrastructure

```bash
# Check all containers running
docker-compose ps

# Should show all services as "Up"

# Test PostgreSQL
docker exec -it hospital-postgres psql -U postgres -c "SELECT version();"

# Test Redis
docker exec -it hospital-redis redis-cli ping
# Should return: PONG

# Test Consul
curl http://localhost:8500/v1/status/leader
# Should return: "127.0.0.1:8300"

# Test RabbitMQ
curl -u admin:admin123 http://localhost:15672/api/overview
```

### Stop Infrastructure

```bash
cd docker
docker-compose down        # Stop and remove containers
docker-compose down -v     # Stop and remove volumes (deletes data!)
```

---

## Database Setup

### Run Flyway Migrations

The project uses Flyway for database migrations. Migrations are located in:
```
hospital-common-lib/src/main/resources/db/migration/
├── V001__create_rbac_tables.sql
├── V002__add_tenant_columns_to_entities.sql
├── V003__seed_system_permissions.sql
├── V004__seed_system_roles.sql
├── V005__enable_row_level_security.sql
├── V006__create_compliance_tables.sql
└── V007__create_audit_logs_table.sql
```

**Automatic Migration** (when service starts):
```bash
# Migrations run automatically on first service startup
./gradlew :auth-service:quarkusDev
# Flyway runs all pending migrations
```

**Manual Migration** (recommended):
```bash
# From project root
cd hospital-common-lib
../gradlew flywayMigrate
cd ..
```

**Manual Migration via Docker** (alternative):
```bash
# Using Flyway CLI
docker run --rm --network=hospital-network \
  -v $(pwd)/hospital-common-lib/src/main/resources/db/migration:/flyway/sql \
  flyway/flyway:10 \
  -url=jdbc:postgresql://hospital-postgres:5432/hospital_db \
  -user=postgres \
  -password=postgres \
  migrate
```

### Database Schema

After migrations, you'll have:

**RBAC Tables:**
- `roles` - Tenant-specific roles
- `permissions` - Global permissions (resource:action)
- `role_permissions` - Role-permission mapping
- `user_roles` - User-role assignments
- `user_resource_permissions` - Fine-grained resource access

**Compliance Tables:**
- `consents` - Patient consent tracking (DPDPA 2023)
- `data_breach_logs` - Data breach incident tracking
- `consent_audit_trail` - Audit trail for consent actions

**Entity Tables:**
- Multi-tenancy columns added to all tables
- Row-Level Security policies enabled
- Composite indexes on (tenant_id, ...)

### Verify Database

```bash
# Connect to PostgreSQL
docker exec -it hospital-postgres psql -U postgres -d hospital_db

# List all tables
\dt

# Check migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

# Check system permissions
SELECT name, description FROM permissions ORDER BY name;

# Check system roles
SELECT name, description FROM roles WHERE is_system_role = true;

# Exit
\q
```

---

## Service Configuration

### Common Configuration

All services share common configuration patterns.

**Environment Variables:**

Create `.env` file in project root:
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=hospital_db
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Consul
CONSUL_HOST=localhost
CONSUL_PORT=8500

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=admin123

# JWT
JWT_SECRET=your-256-bit-secret-key-change-this-in-production
JWT_ISSUER=hospital-system
```

### Generate JWT Keys

For production, generate RSA key pair:

```bash
# Generate private key
openssl genrsa -out privateKey.pem 2048

# Generate public key
openssl rsa -in privateKey.pem -pubout -out publicKey.pem

# Copy to resources
cp publicKey.pem api-gateway/src/main/resources/
cp publicKey.pem auth-service/src/main/resources/
```

For development, services can use symmetric key (HS256).

---

## Running Services

### Service Startup Order

**1. Infrastructure** (must be running first)
```bash
cd docker
docker-compose up -d
```

**2. API Gateway**
```bash
# From project root
./gradlew :api-gateway:quarkusDev
```
Access: http://localhost:8080

**3. Backend Services**

Start in separate terminals (all from project root):

```bash
# Auth Service (port 8081)
./gradlew :auth-service:quarkusDev

# Audit Service (port 8088)
./gradlew :audit-service:quarkusDev

# Patient Service (port 8082) - pending migration
./gradlew :patient-service:quarkusDev

# Doctor Service (port 8083) - pending migration
./gradlew :doctor-service:quarkusDev

# Appointment Service (port 8084) - pending migration
./gradlew :appointment-service:quarkusDev

# Medical Records Service (port 8085) - pending migration
./gradlew :medical-records-service:quarkusDev

# Facility Service (port 8086) - pending migration
./gradlew :facility-service:quarkusDev

# Notification Service (port 8087) - pending migration
./gradlew :notification-service:quarkusDev
```

### Service Registration with Consul

Each service **automatically registers** with Consul on startup via:
```properties
# In application.properties
quarkus.application.name=patient-service
quarkus.consul-config.agent.host-port=localhost:8500
quarkus.consul-config.enabled=true
```

**Verify registration:**
```bash
# List all registered services
curl http://localhost:8500/v1/catalog/services

# Get instances of a specific service
curl http://localhost:8500/v1/catalog/service/patient-service

# Or use Consul UI
open http://localhost:8500/ui
```

### Running Multiple Instances (Load Balancing)

Run multiple instances of a service on different ports:

```bash
# Patient service - instance 1
QUARKUS_HTTP_PORT=8082 ./gradlew :patient-service:quarkusDev

# Patient service - instance 2 (in another terminal)
QUARKUS_HTTP_PORT=8092 ./gradlew :patient-service:quarkusDev

# Patient service - instance 3 (in another terminal)
QUARKUS_HTTP_PORT=8102 ./gradlew :patient-service:quarkusDev
```

All 3 instances register with Consul, and the API Gateway **automatically load balances** across them.

---

## Verification

### Health Checks

**Gateway Health:**
```bash
curl http://localhost:8080/q/health
```

**Backend Service Health:**
```bash
curl http://localhost:8081/q/health  # Auth Service
curl http://localhost:8082/q/health  # Patient Service
curl http://localhost:8083/q/health  # Doctor Service
```

**Expected Response:**
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "Database connection health check",
      "status": "UP"
    },
    {
      "name": "backend-services",
      "status": "UP",
      "data": {
        "auth-service": "UP",
        "patient-service": "UP"
      }
    }
  ]
}
```

### Service Discovery

**Check registered services:**
```bash
curl http://localhost:8080/api/gateway/routes
```

**Expected Response:**
```json
{
  "routes": {
    "/api/auth": "auth-service (via Stork)",
    "/api/patients": "patient-service (via Stork)",
    "/api/doctors": "doctor-service (via Stork)"
  }
}
```

### Test API Gateway

**Public endpoint (no auth):**
```bash
curl http://localhost:8080/
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@hospital.com",
    "password": "admin123"
  }'
```

**Authenticated request:**
```bash
TOKEN="your-jwt-token-from-login"

curl http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

### Test Auth Service (Phase 1 - COMPLETED ✅)

The auth-service is the first migrated service and provides JWT-based authentication.

**1. Register a new user:**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{
    "name": "Test User",
    "email": "test@hospital.com",
    "password": "SecurePass123!",
    "phone": "+1234567890"
  }' | jq
```

**Expected Response (201):**
```json
{
  "id": "uuid",
  "tenantId": "test-tenant",
  "name": "Test User",
  "email": "test@hospital.com",
  "active": true,
  "emailVerified": false,
  "createdAt": "2024-01-01T12:00:00"
}
```

**2. Login with credentials:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{
    "email": "test@hospital.com",
    "password": "SecurePass123!"
  }' | jq
```

**Expected Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "tenantId": "test-tenant",
    "name": "Test User",
    "email": "test@hospital.com",
    "roles": [],
    "permissions": []
  }
}
```

**3. Save and decode JWT token:**
```bash
# Save token to variable
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{"email":"test@hospital.com","password":"SecurePass123!"}' \
  | jq -r '.token')

echo $TOKEN

# Decode JWT (using jwt.io or jwt-cli)
echo $TOKEN | cut -d. -f2 | base64 -d | jq
```

**JWT Claims:**
```json
{
  "sub": "user-uuid",
  "tenantId": "test-tenant",
  "email": "test@hospital.com",
  "name": "Test User",
  "roles": [],
  "permissions": [],
  "iss": "hospital-system",
  "iat": 1234567890,
  "exp": 1234654290
}
```

**4. Test via API Gateway (port 8080):**
```bash
# Login via gateway
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@hospital.com",
    "password": "SecurePass123!"
  }' | jq

# Gateway automatically:
# - Applies rate limiting
# - Adds X-Tenant-Id header from JWT
# - Routes to auth-service via Consul/Stork
```

**5. Verify Consul registration:**
```bash
# Check auth-service is registered
curl http://localhost:8500/v1/catalog/service/auth-service | jq

# Should show:
# - ServiceName: "auth-service"
# - ServicePort: 8081
# - Health: "passing"
```

**6. Test account lockout (security feature):**
```bash
# Try 5 failed login attempts
for i in {1..5}; do
  curl -X POST http://localhost:8081/api/auth/login \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: test-tenant" \
    -d '{"email":"test@hospital.com","password":"wrongpassword"}' | jq
done

# 6th attempt should return 403 Forbidden:
# "Account is temporarily locked. Please try again later."
```

**Auth Service Features Tested:**
- ✅ User registration with validation
- ✅ BCrypt password hashing
- ✅ JWT generation with claims
- ✅ Multi-tenancy (X-Tenant-Id header)
- ✅ Account lockout after failed attempts
- ✅ Consul service registration
- ✅ Reactive Panache queries
- ✅ RBAC integration (roles/permissions)

**See full documentation:** `auth-service/README.md`

---

## Development Workflow

### Development Mode (Live Reload)

Quarkus Dev Mode supports live reload:

```bash
# From project root
./gradlew :patient-service:quarkusDev
```

**Features:**
- Auto-reload on code changes
- Dev UI: http://localhost:8082/q/dev
- Continuous testing: Press `r` to run tests
- Database inspection
- Configuration editor

### Building for Production

**JVM Mode:**
```bash
./gradlew clean build -x test
java -jar patient-service/build/quarkus-app/quarkus-run.jar
```

**Native Mode** (requires GraalVM):
```bash
./gradlew :patient-service:build -Dquarkus.package.type=native -x test
./patient-service/build/patient-service-1.0.0-runner
```

**Native startup:** ~0.5-1s (vs ~3-5s JVM)
**Native memory:** ~50-80MB (vs ~150-200MB JVM)

### Docker Build

**JVM Image:**
```bash
./gradlew :patient-service:build -x test
docker build -f patient-service/src/main/docker/Dockerfile.jvm -t patient-service:jvm patient-service/
```

**Native Image:**
```bash
./gradlew :patient-service:build -Dquarkus.package.type=native -Dquarkus.native.container-build=true -x test
docker build -f patient-service/src/main/docker/Dockerfile.native -t patient-service:native patient-service/
```

### Testing

**Unit Tests:**
```bash
./gradlew test
```

**Integration Tests:**
```bash
./gradlew check
```

**Continuous Testing (Dev Mode):**
```bash
./gradlew :auth-service:quarkusDev
# Press 'r' to run tests
# Press 'o' to toggle test output
```

---

## Monitoring

### Prometheus Metrics

All services expose metrics at `/q/metrics`:

```bash
curl http://localhost:8080/q/metrics  # Gateway
curl http://localhost:8082/q/metrics  # Patient Service
```

**Prometheus UI:**
http://localhost:9090

**Query examples:**
- `http_server_requests_total` - Total requests
- `http_server_requests_duration_seconds` - Request latency
- `jvm_memory_used_bytes` - Memory usage

### Grafana Dashboards

**Access:** http://localhost:3000 (admin/admin)

**Add Prometheus Data Source:**
1. Configuration → Data Sources → Add Prometheus
2. URL: `http://prometheus:9090`
3. Save & Test

**Import Dashboards:**
- Quarkus Dashboard: ID `14370`
- JVM Dashboard: ID `4701`

### Consul Monitoring

**Consul UI:** http://localhost:8500/ui

View:
- Service health status
- Service instances
- Key/Value store
- Access control

### RabbitMQ Monitoring

**Management UI:** http://localhost:15672
**Credentials:** admin / admin123

View:
- Queues and exchanges
- Message rates
- Connections
- Channels

---

## Troubleshooting

### Port Already in Use

**Problem:** `Address already in use: bind`

**Solution:**
```bash
# Find process using port
lsof -i :8080
# Or
netstat -tulpn | grep 8080

# Kill process
kill -9 <PID>

# Or use different port
QUARKUS_HTTP_PORT=8090 ./gradlew :api-gateway:quarkusDev
```

### Database Connection Failed

**Problem:** `Connection refused` or `Connection timeout`

**Solution:**
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs hospital-postgres

# Restart PostgreSQL
docker-compose restart postgres

# Verify connection
docker exec -it hospital-postgres psql -U postgres -c "SELECT 1;"
```

### Consul Service Discovery Not Working

**Problem:** Services not discovered or "Service unavailable"

**Solution:**
```bash
# Check Consul is running
curl http://localhost:8500/v1/status/leader

# Check service registration
curl http://localhost:8500/v1/catalog/services

# Check backend service logs for registration errors
./gradlew :auth-service:quarkusDev | grep -i consul

# Verify Consul config in application.properties
grep consul auth-service/src/main/resources/application.properties
```

### RabbitMQ Connection Failed

**Problem:** Events not being published/consumed

**Solution:**
```bash
# Check RabbitMQ is running
docker ps | grep rabbitmq

# Check RabbitMQ logs
docker logs hospital-rabbitmq

# Verify connection
curl -u admin:admin123 http://localhost:15672/api/overview

# Check queues
curl -u admin:admin123 http://localhost:15672/api/queues
```

### Native Build Fails

**Problem:** `Error during native compilation`

**Solution:**
```bash
# Add @RegisterForReflection to event classes
@RegisterForReflection
public class PatientCreatedEvent { ... }

# Check reflection-config.json
cat auth-service/src/main/resources/reflection-config.json

# Clean and rebuild
./gradlew clean :auth-service:build -Dquarkus.package.type=native -x test
```

### Health Check Fails

**Problem:** Service shows as "DOWN"

**Solution:**
```bash
# Check detailed health
curl http://localhost:8081/q/health | jq

# Check specific health check
curl http://localhost:8081/q/health/live
curl http://localhost:8081/q/health/ready

# View service logs
./gradlew :auth-service:quarkusDev | grep -i health
```

---

## Environment-Specific Configuration

### Development

Use `application.properties` with local settings:
```properties
quarkus.datasource.reactive.url=postgresql://localhost:5432/hospital_db
quarkus.redis.hosts=redis://localhost:6379
quarkus.consul-config.agent.host-port=localhost:8500
```

### Staging

Use `application-staging.properties`:
```properties
quarkus.datasource.reactive.url=postgresql://staging-db:5432/hospital_db
quarkus.redis.hosts=redis://staging-redis:6379
quarkus.consul-config.agent.host-port=staging-consul:8500
```

Run with:
```bash
./gradlew :auth-service:quarkusDev -Dquarkus.profile=staging
```

### Production

Use environment variables (preferred):
```bash
export QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://prod-db:5432/hospital_db
export QUARKUS_REDIS_HOSTS=redis://prod-redis:6379
export CONSUL_HOST=prod-consul
export JWT_SECRET=<strong-secret-key>

java -jar target/quarkus-app/quarkus-run.jar
```

---

## Next Steps

1. ✅ **Infrastructure Running** - All Docker containers up
2. ✅ **Database Migrated** - Flyway migrations applied (V001-V007)
3. ✅ **RBAC & Compliance** - Multi-tenancy, RBAC, DPDPA compliance
4. ✅ **API Gateway** - Vert.x routing with Consul/Stork service discovery
5. ✅ **Auth Service (Phase 1)** - JWT authentication with multi-tenancy
6. 🔄 **Migrate Patient Service (Phase 2)** - Core business entity
7. 🔄 **Migrate Doctor Service (Phase 2)** - Provider management
8. 🔄 **Migrate Appointment Service (Phase 2)** - Snapshot pattern validation
9. 🔄 **Test End-to-End** - Login → Create Patient → Create Appointment
10. 🔄 **Deploy to Production** - K8s/Docker Swarm deployment

### Migration Progress

**Phase 0.5:** ✅ **COMPLETE** - Architecture Updates
- Multi-tenancy infrastructure
- Dynamic RBAC with resource-level permissions
- DPDPA 2023 compliance (consent management, breach tracking)
- Composition-based event design
- API Gateway with service discovery

**Phase 1:** ✅ **COMPLETE** - Auth Service
- User entity with tenant isolation
- JWT generation with roles/permissions
- BCrypt password hashing
- Account lockout security
- Consul service registration
- Integration tests

**Phase 2:** 🔄 **IN PROGRESS** - Core Services
- Patient Service (pending)
- Doctor Service (pending)
- Appointment Service (pending)

**See detailed progress:** `PROJECT_STATUS.md`

---

## Additional Resources

### Documentation

- **Project Status:** `PROJECT_STATUS.md`
- **Troubleshooting:** `TROUBLESHOOTING.md`
- **API Gateway:** `api-gateway/README.md`
- **Auth Service:** `auth-service/README.md` ⭐ **Phase 1 Complete**
- **Audit Service:** `audit-service/README.md` ⭐ **Operational**

### Service Ports

| Service | Port | Status | Documentation |
|---------|------|--------|---------------|
| API Gateway | 8080 | ✅ Running | `api-gateway/README.md` |
| Auth Service | 8081 | ✅ Running | `auth-service/README.md` |
| Audit Service | 8088 | ✅ Running | `audit-service/README.md` |
| Patient Service | 8082 | 🔄 Pending | - |
| Doctor Service | 8083 | 🔄 Pending | - |
| Appointment Service | 8084 | 🔄 Pending | - |
| Medical Records | 8085 | 🔄 Pending | - |
| Facility Service | 8086 | 🔄 Pending | - |
| Notification Service | 8087 | 🔄 Pending | - |

### External Resources

- **Quarkus Guides:** https://quarkus.io/guides/
- **Stork Documentation:** https://smallrye.io/smallrye-stork/
- **Consul Documentation:** https://www.consul.io/docs
- **Mutiny Documentation:** https://smallrye.io/smallrye-mutiny/

---

## Support

**Issues:** Report at GitHub repository
**Questions:** Check migration plan or Quarkus guides
**Performance:** Monitor via Prometheus/Grafana
