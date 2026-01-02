# Hospital Management System - Microservices Architecture

A comprehensive hospital management system built with Spring Boot microservices architecture, featuring **PostgreSQL**, **Redis caching**, **RabbitMQ event-driven messaging**, and **Spring Cloud** infrastructure.

## Architecture Overview

```
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │     :8080       │
                                    └────────┬────────┘
                                             │
              ┌──────────────────────────────┼──────────────────────────────┐
              │                              │                              │
    ┌─────────▼─────────┐         ┌─────────▼─────────┐         ┌─────────▼─────────┐
    │   Auth Service    │         │  Patient Service  │         │  Doctor Service   │
    │      :8081        │         │      :8082        │         │      :8083        │
    └───────────────────┘         └───────────────────┘         └───────────────────┘
              │                              │                              │
    ┌─────────▼─────────┐         ┌─────────▼─────────┐         ┌─────────▼─────────┐
    │Appointment Service│         │Medical Records Svc│         │ Facility Service  │
    │      :8084        │         │      :8085        │         │      :8086        │
    └───────────────────┘         └───────────────────┘         └───────────────────┘
              │                              │                              │
    ┌─────────▼─────────┐         ┌─────────▼─────────┐                     │
    │Notification Service│        │   Audit Service   │◄────────────────────┘
    │      :8087        │         │      :8088        │
    └───────────────────┘         └───────────────────┘
              │                              │
              └──────────────┬───────────────┘
                             │
    ┌────────────────────────┼────────────────────────┐
    │                        │                        │
    ▼                        ▼                        ▼
┌─────────┐           ┌────────────┐           ┌──────────┐
│PostgreSQL│          │  RabbitMQ  │           │  Redis   │
│  :5432  │           │:5672/:15672│           │  :6379   │
└─────────┘           └────────────┘           └──────────┘
```

## Services Overview

### Microservices (8 Services)

| Service | Port | Description |
|---------|------|-------------|
| Auth Service | 8081 | HS512 JWT authentication, user management |
| Patient Service | 8082 | Patient demographics and health records |
| Doctor Service | 8083 | Doctor profiles and specialties |
| Appointment Service | 8084 | Scheduling with disease-specialty matching |
| Medical Records Service | 8085 | Records, prescriptions, medical reports |
| Facility Service | 8086 | Room management and admissions (Saga pattern) |
| Notification Service | 8087 | Event-driven email notifications |
| Audit Service | 8088 | HIPAA-compliant audit logging |

### Infrastructure Components

| Component | Port | Description |
|-----------|------|-------------|
| API Gateway | 8080 | Single entry point, JWT validation, routing |
| Eureka Server | 8761 | Service discovery and registration |
| PostgreSQL | 5432 | Database (separate DB per service) |
| Redis | 6379 | Distributed caching |
| RabbitMQ | 5672/15672 | Event-driven messaging |

## Technology Stack

- **Java 21** with modern features
- **Spring Boot 3.4.1**
- **Spring Cloud 2024.0.0** (Gateway, Eureka, OpenFeign)
- **PostgreSQL 16** with UUID primary keys
- **Redis 7** for distributed caching
- **RabbitMQ 3.13** for event messaging
- **Docker Compose** for infrastructure
- **Maven** multi-module project

---

## Quick Start

### Prerequisites

- **Java 21** (required)
- **Maven 3.9+** (or use included `./mvnw`)
- **Docker & Docker Compose** (for infrastructure)

### Step 1: Clone and Build

```bash
# Clone the repository
git clone https://github.com/SanathAishu/clinic-mgmt.git
cd clinic-mgmt

# Build all services
./mvnw clean package -DskipTests
```

### Step 2: Start Everything

The easiest way to start the entire system:

```bash
# Start infrastructure (PostgreSQL, Redis, RabbitMQ) and all services
./start-local.sh
```

This script will:
1. Start Docker containers for PostgreSQL, Redis, and RabbitMQ
2. Wait for infrastructure to be ready
3. Create all required databases automatically
4. Start Eureka Server and wait for it to be ready
5. Start API Gateway and all 8 microservices
6. Auto-declare all RabbitMQ exchanges and queues

### Step 3: Verify Startup

Wait about 60 seconds for all services to register, then verify:

```bash
# Check Eureka for registered services
curl http://localhost:8761/actuator/health

# Check API Gateway
curl http://localhost:8080/actuator/health

# View RabbitMQ Management UI
open http://localhost:15672  # Login: guest/guest
```

### Step 4: Stop Everything

```bash
./stop-local.sh

# To also stop infrastructure:
docker-compose down
```

---

## Detailed Setup Instructions

### Infrastructure Only (Docker)

The `docker-compose.yml` starts only the infrastructure components:

```bash
# Start infrastructure
docker-compose up -d

# Verify all containers are healthy
docker-compose ps

# Expected output:
# hospital-postgres   Up (healthy)
# hospital-rabbitmq   Up (healthy)
# hospital-redis      Up (healthy)
```

### Database Setup

PostgreSQL automatically creates 8 databases on first startup via `docker/postgres/init-databases.sh`:

| Database | Service |
|----------|---------|
| auth_service | Auth Service |
| patient_service | Patient Service |
| doctor_service | Doctor Service |
| appointment_service | Appointment Service |
| medical_records_service | Medical Records Service |
| facility_service | Facility Service |
| notification_service | Notification Service |
| audit_service | Audit Service |

**Credentials:** `postgres` / `postgres`

### Manual Service Startup

If you prefer to start services manually:

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Wait for databases to be ready (first time only)
sleep 15

# 3. Build if needed
./mvnw clean package -DskipTests

# 4. Start Eureka Server first
java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar &

# 5. Wait for Eureka (check http://localhost:8761)
sleep 30

# 6. Start API Gateway
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar &

# 7. Start remaining services (can be parallel)
java -jar auth-service/target/auth-service-1.0.0-SNAPSHOT.jar &
java -jar patient-service/target/patient-service-1.0.0-SNAPSHOT.jar &
java -jar doctor-service/target/doctor-service-1.0.0-SNAPSHOT.jar &
java -jar appointment-service/target/appointment-service-1.0.0-SNAPSHOT.jar &
java -jar medical-records-service/target/medical-records-service-1.0.0-SNAPSHOT.jar &
java -jar facility-service/target/facility-service-1.0.0-SNAPSHOT.jar &
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar &
java -jar audit-service/target/audit-service-1.0.0-SNAPSHOT.jar &
```

### Service Logs

When using `start-local.sh`, logs are written to:

```bash
logs/
├── eureka-server.log
├── api-gateway.log
├── auth-service.log
├── patient-service.log
├── doctor-service.log
├── appointment-service.log
├── medical-records-service.log
├── facility-service.log
├── notification-service.log
└── audit-service.log

# Tail a specific service log
tail -f logs/patient-service.log

# Search for errors
grep -i error logs/*.log
```

---

## API Usage

### Base URL

All requests go through the API Gateway:
```
http://localhost:8080
```

### Authentication Flow

#### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@hospital.com",
    "password": "Admin123456",
    "name": "Admin User",
    "role": "ADMIN",
    "gender": "MALE"
  }'
```

**Available Roles:** `ADMIN`, `DOCTOR`, `PATIENT`, `NURSE`, `RECEPTIONIST`

#### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@hospital.com",
    "password": "Admin123456"
  }'
```

Response includes JWT token:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

#### 3. Use Token for Protected Endpoints

```bash
# Set token variable
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Make authenticated request
curl http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

### API Endpoints

#### Public Routes (No Authentication)
```
POST /api/auth/register    - Register new user
POST /api/auth/login       - Login and get JWT
GET  /actuator/health      - Health check
```

#### Protected Routes (Requires JWT)

**Patients**
```
GET    /api/patients           - List all patients
GET    /api/patients/{id}      - Get patient by ID
POST   /api/patients           - Create patient
PUT    /api/patients/{id}      - Update patient
DELETE /api/patients/{id}      - Delete patient
```

**Doctors**
```
GET    /api/doctors                      - List all doctors
GET    /api/doctors/{id}                 - Get doctor by ID
POST   /api/doctors                      - Create doctor
GET    /api/doctors/specialty/{specialty} - Get by specialty
```

**Appointments**
```
POST   /api/appointments              - Book appointment
GET    /api/appointments/{id}         - Get appointment
GET    /api/appointments/patient/{id} - Get patient appointments
PUT    /api/appointments/{id}/cancel  - Cancel appointment
```

**Medical Records**
```
GET    /api/medical-records/patient/{id}  - Get patient records
POST   /api/medical-records               - Create medical record
POST   /api/prescriptions                 - Create prescription
POST   /api/medical-reports               - Create report
```

**Facility**
```
GET    /api/rooms              - List available rooms
POST   /api/rooms              - Create room (Admin)
POST   /api/room-bookings      - Book a room (Admission)
```

**Audit (Admin only)**
```
GET    /api/audit/logs         - Get audit logs
```

---

## Configuration

### Environment Variables

All configuration is embedded in each service's `application.yml`. The `start-local.sh` script overrides key settings via command-line arguments:

| Variable | Default | Description |
|----------|---------|-------------|
| `server.port` | Per service | Service port |
| `spring.datasource.url` | Per service | PostgreSQL connection |
| `spring.datasource.username` | postgres | DB username |
| `spring.datasource.password` | postgres | DB password |
| `spring.rabbitmq.host` | localhost | RabbitMQ host |
| `spring.rabbitmq.username` | guest | RabbitMQ user |
| `spring.rabbitmq.password` | guest | RabbitMQ password |
| `spring.data.redis.host` | localhost | Redis host |
| `jwt.secret` | (64+ chars) | HS512 JWT secret |
| `eureka.client.service-url.defaultZone` | http://localhost:8761/eureka/ | Eureka URL |

### JWT Configuration

The system uses **HS512** algorithm with a shared secret key across all services:

```
HospitalManagementSystemSecretKeyForHS512Algorithm2024SecureTokenGeneration
```

**Important:** Change this in production!

---

## Project Structure

```
clinic-mgmt/
├── pom.xml                      # Parent POM (multi-module)
├── docker-compose.yml           # Infrastructure (PostgreSQL, Redis, RabbitMQ)
├── start-local.sh               # Start all services locally
├── stop-local.sh                # Stop all services
│
├── docker/
│   └── postgres/
│       └── init-databases.sh    # Creates all 8 databases
│
├── hospital-common/             # Shared library (included by all services)
│   ├── config/                  # RabbitMQ, Redis configurations
│   ├── dto/                     # Common DTOs, ApiResponse
│   ├── enums/                   # Disease, Specialty, Gender, Role
│   ├── events/                  # Domain events for RabbitMQ
│   └── exception/               # Common exception handling
│
├── eureka-server/               # Service Discovery (:8761)
├── api-gateway/                 # API Gateway + JWT Filter (:8080)
│
├── auth-service/                # Authentication (:8081)
├── patient-service/             # Patient Management (:8082)
├── doctor-service/              # Doctor Management (:8083)
├── appointment-service/         # Appointments + Snapshots (:8084)
├── medical-records-service/     # Medical Records (:8085)
├── facility-service/            # Rooms + Saga (:8086)
├── notification-service/        # Email Notifications (:8087)
└── audit-service/               # Audit Logging (:8088)
```

---

## Key Features

### Security
- **HS512 JWT** with shared secret key across services
- API Gateway validates tokens before routing
- Role-based access control (ADMIN, DOCTOR, PATIENT, NURSE, RECEPTIONIST)

### Event-Driven Architecture
- RabbitMQ topic/direct exchanges for async communication
- Auto-declaration of all queues and exchanges on startup
- Domain events for cross-service data sync (patient/doctor snapshots)
- Cache invalidation events
- Saga pattern for distributed transactions (facility admissions)

### Disease-Specialty Matching
- Intelligent doctor recommendation based on patient's disease
- 24 diseases mapped to medical specialties
- Automatic specialty filtering for appointments

### Distributed Caching
- Redis with per-entity TTL configurations
- Cache-aside pattern implementation
- Cross-service cache invalidation via RabbitMQ events

### Audit Logging
- Comprehensive audit logging in Audit Service
- All create/update/delete operations tracked via RabbitMQ events
- Separate audit queues for each domain

---

## Troubleshooting

### Services Not Starting

1. **Check if infrastructure is running:**
   ```bash
   docker-compose ps
   ```

2. **Check service logs:**
   ```bash
   tail -100 logs/<service-name>.log
   ```

3. **Verify Eureka registration:**
   ```bash
   curl http://localhost:8761/eureka/apps
   ```

### Database Connection Issues

1. **Verify PostgreSQL is running:**
   ```bash
   docker exec hospital-postgres psql -U postgres -c "\l"
   ```

2. **Check if databases exist:**
   ```bash
   docker exec hospital-postgres psql -U postgres -c "\l" | grep service
   ```

3. **Recreate databases (if needed):**
   ```bash
   docker-compose down -v  # Removes volumes
   docker-compose up -d    # Recreates with fresh data
   ```

### RabbitMQ Queue Issues

1. **Check RabbitMQ Management UI:**
   ```
   http://localhost:15672 (guest/guest)
   ```

2. **Verify queues exist:**
   ```bash
   curl -s -u guest:guest http://localhost:15672/api/queues | jq '.[].name'
   ```

3. **Queues are auto-declared** when services start. If missing, restart the services.

### Port Already in Use

```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use stop script
./stop-local.sh
```

---

## Development

### Building a Single Service

```bash
./mvnw clean package -DskipTests -pl patient-service -am
```

### Running Tests

```bash
# All tests
./mvnw test

# Specific service
./mvnw test -pl patient-service
```

### Adding a New Service

1. Create new module directory
2. Add to parent `pom.xml` modules
3. Include `hospital-common` dependency
4. Add `@SpringBootApplication(scanBasePackages = {"com.hospital.<service>", "com.hospital.common"})`
5. Add to `start-local.sh`
6. Add database to `docker/postgres/init-databases.sh`

---

## License

Proprietary - Hospital Management System
