# Hospital Management System - Quick Start Guide

**Get the system running and test auth-service in 10 minutes.**

---

## Step 1: Prerequisites Check (1 minute)

```bash
# Verify required tools
java -version      # Must be 21+
./gradlew --version  # Gradle wrapper (included in project)
docker --version
docker-compose --version
```

---

## Step 2: Start Infrastructure (2 minutes)

```bash
# Clone and navigate to project
cd Clinic-Mgmt-Quarkus

# Start all infrastructure services
cd docker
docker-compose up -d
cd ..

# Verify all services are up
docker-compose ps

# Expected: All services should show "Up" status
```

**Services Started:**
- ✅ PostgreSQL (port 5432)
- ✅ Redis (port 6379)
- ✅ Consul (port 8500)
- ✅ RabbitMQ (ports 5672, 15672)
- ✅ Prometheus (port 9090)
- ✅ Grafana (port 3000)

---

## Step 3: Run Database Migrations (1 minute)

```bash
# Navigate to hospital-common-lib (shared library module)
cd hospital-common-lib

# Run Flyway migrations
../gradlew flywayMigrate

# Go back to root
cd ..

# Verify migrations
docker exec -it hospital-postgres psql -U postgres -d hospital_db -c \
  "SELECT installed_rank, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

**Expected Output:**
```
 installed_rank |              description              | success
----------------+---------------------------------------+---------
              1 | create rbac tables                    | t
              2 | add tenant columns to entities        | t
              3 | seed system permissions               | t
              4 | seed system roles                     | t
              5 | enable row level security             | t
              6 | create compliance tables              | t
              7 | create audit logs table               | t
```

---

## Step 4: Build All Modules (2 minutes)

```bash
# From project root (Clinic-Mgmt-Quarkus)

# Build all modules
./gradlew clean build -x test

# Expected output:
# > Task :hospital-common-lib:build
# > Task :auth-service:build
# > Task :audit-service:build
# > Task :api-gateway:build
# BUILD SUCCESSFUL
```

---

## Step 5: Start Auth Service (1 minute)

```bash
# Terminal 1: Start auth-service (from project root)
./gradlew :auth-service:quarkusDev
```

**Wait for:**
```
Listening for transport dt_socket at address: 5005
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2024-01-01 12:00:00,000 INFO  [io.quarkus] (Quarkus Main Thread) auth-service 1.0.0 on JVM started in 2.345s.
2024-01-01 12:00:00,001 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2024-01-01 12:00:00,002 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [...]

Tests paused
Press [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```

✅ **Auth Service Running:** http://localhost:8081

---

## Step 6: Start API Gateway (1 minute)

```bash
# Terminal 2: Start API Gateway (from project root)
./gradlew :api-gateway:quarkusDev
```

✅ **API Gateway Running:** http://localhost:8080

---

## Step 7: Verify Consul Registration (30 seconds)

```bash
# Check registered services
curl http://localhost:8500/v1/catalog/services | jq

# Expected output:
{
  "consul": [],
  "auth-service": []
}

# Verify auth-service details
curl http://localhost:8500/v1/catalog/service/auth-service | jq

# Should show:
# - ServiceName: "auth-service"
# - ServicePort: 8081
# - Health checks: "passing"
```

**Consul UI:** http://localhost:8500/ui

---

## Step 8: Test Auth Service (2 minutes)

### 8.1 Register a User

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

**Expected (201 Created):**
```json
{
  "id": "uuid",
  "tenantId": "test-tenant",
  "name": "Test User",
  "email": "test@hospital.com",
  "active": true,
  "emailVerified": false
}
```

### 8.2 Login

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{
    "email": "test@hospital.com",
    "password": "SecurePass123!"
  }' | jq
```

**Expected (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "name": "Test User",
    "email": "test@hospital.com",
    "roles": [],
    "permissions": []
  }
}
```

### 8.3 Test via API Gateway

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@hospital.com",
    "password": "SecurePass123!"
  }' | jq
```

✅ **Gateway routes request to auth-service via Consul/Stork**

---

## Step 9: Verify Health Checks (30 seconds)

```bash
# Auth Service Health
curl http://localhost:8081/q/health | jq

# API Gateway Health
curl http://localhost:8080/q/health | jq

# Both should return:
{
  "status": "UP",
  "checks": [...]
}
```

---

## Step 10: Access Monitoring UIs

**Consul UI:**
- URL: http://localhost:8500/ui
- Services: Should show `auth-service` registered
- Health: Should show "passing"

**RabbitMQ UI:**
- URL: http://localhost:15672
- Credentials: `admin` / `admin123`
- Queues: Will show user-events when publishing

**Prometheus:**
- URL: http://localhost:9090
- Metrics: Search for `auth` to see auth-service metrics

**Grafana:**
- URL: http://localhost:3000
- Credentials: `admin` / `admin`
- Dashboards: Import Quarkus dashboard (ID: 14370)

---

## ✅ System Validation Checklist

- [ ] Infrastructure services running (PostgreSQL, Redis, Consul, RabbitMQ)
- [ ] Database migrations applied (7 migrations)
- [ ] Auth service builds successfully
- [ ] Auth service starts in dev mode
- [ ] API Gateway starts in dev mode
- [ ] Auth service registers with Consul
- [ ] User registration works (201 Created)
- [ ] User login works (200 OK with JWT)
- [ ] Login via gateway works (routes correctly)
- [ ] Health checks return "UP"
- [ ] Consul UI shows auth-service
- [ ] JWT token contains correct claims

---

## 🎯 Next Steps

### Test Advanced Features

**1. Test Account Lockout:**
```bash
# Try 5 failed login attempts
for i in {1..6}; do
  curl -X POST http://localhost:8081/api/auth/login \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: test-tenant" \
    -d '{"email":"test@hospital.com","password":"wrongpassword"}' | jq
done

# 6th attempt should return 403 Forbidden
```

**2. Test Multi-Tenancy:**
```bash
# Register user in tenant A
curl -X POST http://localhost:8081/api/auth/register \
  -H "X-Tenant-Id: tenant-a" \
  -d '{"name":"User A","email":"user@example.com","password":"SecurePass123!"}' | jq

# Register same email in tenant B (should succeed - different tenant)
curl -X POST http://localhost:8081/api/auth/register \
  -H "X-Tenant-Id: tenant-b" \
  -d '{"name":"User B","email":"user@example.com","password":"SecurePass123!"}' | jq
```

**3. Decode JWT Token:**
```bash
# Get token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: test-tenant" \
  -d '{"email":"test@hospital.com","password":"SecurePass123!"}' \
  | jq -r '.token')

# Decode payload (base64)
echo $TOKEN | cut -d. -f2 | base64 -d | jq
```

**Expected JWT Claims:**
```json
{
  "sub": "user-uuid",
  "tenantId": "test-tenant",
  "email": "test@hospital.com",
  "name": "Test User",
  "roles": [],
  "permissions": [],
  "iss": "hospital-system",
  "exp": 1234567890
}
```

### Run Integration Tests

```bash
# From project root
./gradlew :auth-service:test

# Should run AuthControllerTest with 5 tests
# All tests should pass
```

### Build Native Image (Optional)

```bash
# From project root
./gradlew :auth-service:build -Dquarkus.package.type=native -x test

# Native binary: auth-service/build/auth-service-1.0.0-runner
# Startup: ~0.5s (vs 3-5s JVM)
# Memory: ~50MB (vs 150MB JVM)
```

---

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Find process
lsof -i :8081

# Kill process
kill -9 <PID>
```

### Database Connection Failed
```bash
# Check PostgreSQL
docker ps | grep postgres
docker logs hospital-postgres

# Restart
docker-compose restart postgres
```

### Consul Not Running
```bash
# Check Consul
curl http://localhost:8500/v1/status/leader

# Restart
docker-compose restart consul
```

### Service Won't Start
```bash
# Clean build (from project root)
./gradlew :auth-service:clean :auth-service:build

# Check logs
./gradlew :auth-service:quarkusDev

# Look for errors in red
```

---

## 📚 Documentation

- **Complete Setup:** `SETUP.md`
- **Troubleshooting:** `TROUBLESHOOTING.md`
- **Project Status:** `PROJECT_STATUS.md`
- **Auth Service:** `auth-service/README.md`
- **Audit Service:** `audit-service/README.md`
- **API Gateway:** `api-gateway/README.md`

---

## ✨ Success!

You now have:
- ✅ Infrastructure running (PostgreSQL, Redis, Consul, RabbitMQ)
- ✅ Database with RBAC and compliance tables
- ✅ Auth Service with JWT authentication
- ✅ API Gateway with service discovery
- ✅ Multi-tenancy support
- ✅ Account security (lockout, password validation)
- ✅ Reactive architecture (Mutiny Uni/Multi)

**Ready for Phase 2:** Migrate Patient, Doctor, and Appointment services!
