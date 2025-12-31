# Hospital Management System - Microservices Architecture

A comprehensive hospital management system built with Spring Boot microservices architecture, featuring **PostgreSQL with UUID primary keys**, **Redis caching**, **RabbitMQ event-driven messaging**, and **Spring Cloud** infrastructure.

## Architecture Overview

### Microservices (8 Services)
1. **Auth Service** (8081) - RS256 JWT authentication, user management
2. **Patient Service** (8082) - Patient demographics and health records
3. **Doctor Service** (8083) - Doctor profiles and specialties
4. **Appointment Service** (8084) - Appointment scheduling with validation
5. **Medical Records Service** (8085) - Medical records, prescriptions, reports
6. **Facility Service** (8086) - Room management and admissions (Saga pattern)
7. **Notification Service** (8087) - Event-driven email notifications
8. **Audit Service** (8088) - HIPAA-compliant audit logging with JSONB

### Infrastructure Components
- **Config Server** (8888) - Centralized configuration management
- **Eureka Server** (8761) - Service discovery and registration
- **API Gateway** (8080) - Single entry point, JWT validation, routing
- **PostgreSQL** (5432) - Database with 8 schemas, UUID support
- **Redis** (6379) - Distributed caching
- **RabbitMQ** (5672, 15672) - Event-driven messaging

## Technology Stack

- **Java 21** with preview features
- **Spring Boot 3.5.0**
- **Spring Cloud 2024.0.0**
- **PostgreSQL 16 Alpine** with UUID extensions
- **Redis 7 Alpine**
- **RabbitMQ 3.13 Management Alpine**
- **Docker Compose** for local development
- **Maven** multi-module project

## Key Features

### UUID Primary Keys
- All entities use UUID as primary keys (`gen_random_uuid()`)
- Better for distributed systems and security
- No sequential ID exposure

### PostgreSQL JSONB
- Flexible metadata storage
- Full-text search with GIN indexes
- Audit logs with JSONB columns

### Event-Driven Architecture
- RabbitMQ for asynchronous communication
- Domain events for data synchronization
- Cache invalidation events
- Saga pattern for distributed transactions

### Security
- RS256 JWT (asymmetric encryption)
- Auth Service holds private key
- Other services validate with public key
- Role-based access control (RBAC)

### Distributed Caching
- Redis for shared caching across services
- Different TTLs for different entities
- Cache-aside pattern
- Event-driven cache invalidation

### HIPAA Compliance
- Comprehensive audit logging
- 7-year retention policy
- Table partitioning for performance
- JSONB for flexible audit data

## Project Structure

```
.
├── pom.xml                          # Parent POM
├── hospital-common/                 # Shared library
│   ├── dto/                         # Common DTOs
│   ├── enums/                       # Enums (Disease, Specialty, etc.)
│   ├── events/                      # Domain events
│   ├── exception/                   # Base exceptions
│   ├── config/                      # RabbitMQ, Redis, Feign config
│   └── util/                        # Utilities (DiseaseSpecialtyMapper)
├── config-server/                   # Centralized configuration
├── eureka-server/                   # Service discovery
├── api-gateway/                     # API Gateway with JWT validation
├── config-repo/                     # Configuration files for all services
├── docker-compose.yml               # Infrastructure services
└── infrastructure/
    ├── postgresql/init-schemas.sql  # Database initialization
    └── rabbitmq/rabbitmq.conf       # RabbitMQ configuration
```

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose

### 1. Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, RabbitMQ
docker-compose up -d

# Verify all services are running
docker-compose ps

# Check PostgreSQL schemas
docker exec hospital-postgres psql -U hospital_user -d hospital_db -c "\dn"
```

### 2. Build All Modules

```bash
# Build parent and all modules
./mvnw clean install

# Or build specific module
./mvnw clean install -pl hospital-common
```

### 3. Start Infrastructure Modules (in order)

```bash
# 1. Config Server (must start first)
cd config-server
../mvnw spring-boot:run

# 2. Eureka Server
cd ../eureka-server
../mvnw spring-boot:run

# 3. API Gateway
cd ../api-gateway
../mvnw spring-boot:run
```

### 4. Access Infrastructure

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Config Server**: http://localhost:8888
- **RabbitMQ Management**: http://localhost:15672 (user: hospital_user, pass: rabbitmq_password_2025)

## Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Database
POSTGRES_DB=hospital_db
POSTGRES_USER=hospital_user
POSTGRES_PASSWORD=hospital_password_2025

# Redis
REDIS_PASSWORD=redis_password_2025

# RabbitMQ
RABBITMQ_USER=hospital_user
RABBITMQ_PASSWORD=rabbitmq_password_2025

# JWT (will be generated by Auth Service)
JWT_PRIVATE_KEY_PATH=/path/to/private_key.pem
JWT_PUBLIC_KEY_PATH=/path/to/public_key.pem

# Email (for Notification Service)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

## Database Schemas

PostgreSQL has 8 separate schemas (not databases):

1. **auth_service** - Users, roles, authentication
2. **patient_service** - Patient demographics
3. **doctor_service** - Doctor profiles
4. **appointment_service** - Appointments + patient/doctor snapshots
5. **medical_records_service** - Records, prescriptions, reports
6. **facility_service** - Rooms, admissions, bookings
7. **notification_service** - Email logs, templates
8. **audit_service** - Audit logs with JSONB

All schemas have UUID extensions enabled:
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

## RabbitMQ Configuration

### Exchanges
- `hospital.events.topic` - Topic exchange for notifications
- `hospital.events.direct` - Direct exchange for snapshots
- `hospital.events.saga` - Direct exchange for sagas

### Queues
- `appointment.notifications`
- `prescription.notifications`
- `patient.updates` (for snapshots)
- `doctor.updates` (for snapshots)
- `patient.admission.request` (for saga)
- `cache.invalidation`

## API Routes (via Gateway)

All requests go through API Gateway at `http://localhost:8080`

### Public Routes (No Authentication)
- `POST /api/auth/register/patient` - Patient registration
- `POST /api/auth/register/doctor` - Doctor registration
- `POST /api/auth/login` - Login
- `GET /api/auth/public-key` - Get JWT public key

### Protected Routes (Requires JWT)
- `GET/POST/PUT/DELETE /api/patients/**`
- `GET/POST/PUT/DELETE /api/doctors/**`
- `GET/POST/PUT/DELETE /api/appointments/**`
- `GET/POST/PUT/DELETE /api/medical-records/**`
- `GET/POST/PUT/DELETE /api/rooms/**`

## Development Roadmap

### Phase 1: Infrastructure Setup ✅
- [x] Docker Compose (PostgreSQL, Redis, RabbitMQ)
- [x] Parent POM and hospital-common module
- [x] Config Server
- [x] Eureka Server
- [x] API Gateway

### Phase 2: Auth Service (In Progress)
- [ ] User entity with UUID
- [ ] RS256 JWT generation
- [ ] Public key endpoint
- [ ] Registration and login endpoints

### Phase 3: Patient & Doctor Services
- [ ] Patient CRUD with UUID
- [ ] Doctor CRUD with UUID
- [ ] Event publishing (created, updated, deleted)
- [ ] Redis caching

### Phase 4: Appointment Service
- [ ] Appointment CRUD with UUID foreign keys
- [ ] Patient/Doctor snapshot tables
- [ ] Feign clients for validation
- [ ] Event consumers for snapshot updates

### Phase 5: Medical Records Service
- [ ] Medical records with UUID
- [ ] Prescriptions and reports
- [ ] JSONB metadata columns

### Phase 6: Facility Service
- [ ] Room management
- [ ] Saga pattern for admissions
- [ ] Compensation logic

### Phase 7: Notification Service
- [ ] RabbitMQ consumers
- [ ] Email templates
- [ ] SMTP integration

### Phase 8: Audit Service
- [ ] Audit logging with JSONB
- [ ] Table partitioning
- [ ] 7-year retention

### Phase 9: Testing
- [ ] Unit tests
- [ ] Integration tests with TestContainers
- [ ] E2E tests

### Phase 10: Production Deployment
- [ ] Kubernetes manifests
- [ ] CI/CD pipelines
- [ ] Monitoring (Prometheus, Grafana)

## Contributing

This is a migration project from a monolithic architecture. See `microservices-migration.md` for the complete migration plan.

## License

Proprietary - Hospital Management System
