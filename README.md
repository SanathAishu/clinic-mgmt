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
┌────────┐            ┌────────────┐           ┌──────────┐
│PostgreSQL│          │  RabbitMQ  │           │  Redis   │
│  :5432  │           │:5672/:15672│           │  :6379   │
└─────────┘           └────────────┘           └──────────┘
```

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
| Config Server | 8888 | Centralized configuration management |
| Eureka Server | 8761 | Service discovery and registration |
| PostgreSQL | 5432 | Database (separate DB per service) |
| Redis | 6379 | Distributed caching |
| RabbitMQ | 5672/15672 | Event-driven messaging |

## Technology Stack

- **Java 21** with modern features
- **Spring Boot 3.5.0**
- **Spring Cloud 2024.0.0** (Gateway, Eureka, Config, OpenFeign)
- **PostgreSQL 16** with UUID primary keys
- **Redis 7** for distributed caching
- **RabbitMQ 3.13** for event messaging
- **Docker Compose** for local development
- **Maven** multi-module project

## Key Features

### Security
- **HS512 JWT** with shared secret key across services
- API Gateway validates tokens before routing
- Role-based access control (ADMIN, DOCTOR, PATIENT, NURSE, STAFF)

### Event-Driven Architecture
- RabbitMQ topic/direct exchanges for async communication
- Domain events for cross-service data sync
- Cache invalidation events
- Saga pattern for distributed transactions (facility admissions)

### Disease-Specialty Matching
- Intelligent doctor recommendation based on patient's disease
- 95 diseases mapped to 25 medical specialties
- Automatic specialty filtering for appointments

### Distributed Caching
- Redis with per-entity TTL configurations
- Cache-aside pattern implementation
- Cross-service cache invalidation via events

### HIPAA Compliance
- Comprehensive audit logging in Audit Service
- All create/update/delete operations tracked
- Configurable retention policies

## Quick Start

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose

### 1. Start Infrastructure Only

```bash
# Start PostgreSQL, Redis, RabbitMQ
docker-compose -f docker-compose-infra.yml up -d

# Verify services
docker-compose -f docker-compose-infra.yml ps
```

### 2. Build All Services

```bash
./mvnw clean package -DskipTests
```

### 3. Run Services Locally

```bash
# Start in order:
# 1. Config Server
cd config-server && ../mvnw spring-boot:run &

# 2. Eureka Server (wait for config server)
cd eureka-server && ../mvnw spring-boot:run &

# 3. API Gateway
cd api-gateway && ../mvnw spring-boot:run &

# 4. Business services (can start in parallel)
cd auth-service && ../mvnw spring-boot:run &
cd patient-service && ../mvnw spring-boot:run &
cd doctor-service && ../mvnw spring-boot:run &
# ... etc
```

### 4. Or Run Everything with Docker

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all
docker-compose down
```

### 5. Access Services

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |

## API Endpoints

All requests go through API Gateway at `http://localhost:8080`

### Public Routes (No Authentication)
```
POST /api/auth/register    - Register new user
POST /api/auth/login       - Login and get JWT
GET  /api/auth/health      - Health check
```

### Protected Routes (Requires JWT in Authorization header)
```
# Patients
GET    /api/patients           - List all patients
GET    /api/patients/{id}      - Get patient by ID
POST   /api/patients           - Create patient
PUT    /api/patients/{id}      - Update patient
DELETE /api/patients/{id}      - Delete patient

# Doctors
GET    /api/doctors            - List all doctors
GET    /api/doctors/{id}       - Get doctor by ID
GET    /api/doctors/specialty/{specialty} - Get by specialty

# Appointments
POST   /api/appointments       - Book appointment
GET    /api/appointments/{id}  - Get appointment
PUT    /api/appointments/{id}/cancel - Cancel appointment

# Medical Records
GET    /api/medical-records/patient/{id}  - Get patient records
POST   /api/prescriptions      - Create prescription
POST   /api/medical-reports    - Create report

# Facility
GET    /api/rooms              - List available rooms
POST   /api/room-bookings      - Book a room

# Audit (Admin only)
GET    /api/audit/logs         - Get audit logs
```

## Environment Variables

Copy `.env.example` to `.env`:

```bash
# JWT (min 64 chars for HS512)
JWT_SECRET=HospitalManagementSystemSecretKeyForHS512Algorithm2024SecureTokenGeneration

# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=localhost

# Email (optional, for notifications)
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=
MAIL_PASSWORD=
```

## Project Structure

```
.
├── pom.xml                      # Parent POM
├── docker-compose.yml           # Full stack
├── docker-compose-infra.yml     # Infrastructure only
├── .env.example                 # Environment template
│
├── hospital-common/             # Shared library
│   ├── dto/                     # Common DTOs, ApiResponse
│   ├── enums/                   # Disease, Specialty, Role
│   ├── events/                  # Domain events
│   ├── config/                  # RabbitMQ, Redis, Feign
│   └── security/                # Shared JwtUtils
│
├── config-server/               # Spring Cloud Config
├── eureka-server/               # Service Discovery
├── api-gateway/                 # API Gateway + JWT Filter
│
├── auth-service/                # Authentication
├── patient-service/             # Patient Management
├── doctor-service/              # Doctor Management
├── appointment-service/         # Appointments + Snapshots
├── medical-records-service/     # Medical Records
├── facility-service/            # Rooms + Saga
├── notification-service/        # Email Notifications
└── audit-service/               # Audit Logging
```

## Development Status

| Phase | Status |
|-------|--------|
| Phase 1: Infrastructure Setup | ✅ Complete |
| Phase 2: Auth Service | ✅ Complete |
| Phase 3: Patient & Doctor Services | ✅ Complete |
| Phase 4: Appointment Service | ✅ Complete |
| Phase 5: Medical Records Service | ✅ Complete |
| Phase 6: Facility Service (Saga) | ✅ Complete |
| Phase 7: Notification Service | ✅ Complete |
| Phase 8: Audit Service | ✅ Complete |
| Phase 9: Testing | ⏳ Pending |
| Phase 10: Deployment | ⏳ Pending |

## License

Proprietary - Hospital Management System
