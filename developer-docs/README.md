# Hospital Management System - Developer Documentation

A comprehensive hospital management system built with Spring Boot microservices, featuring reactive programming with **Spring WebFlux**, **R2DBC** for non-blocking database access, **Redis caching**, and **Spring Cloud** infrastructure.

## Documentation Index

| Document | Description |
|----------|-------------|
| [Architecture](./architecture.md) | System architecture, service overview, tech stack |
| [Database Setup](./database-setup.md) | PostgreSQL installation, database creation, Flyway migrations |
| [API Reference](./api-reference.md) | REST API endpoints, authentication, request/response examples |

## Quick Start

### Prerequisites

- **Java 21** (required)
- **Maven 3.9+** (or use included `./mvnw`)
- **Docker & Docker Compose** (for PostgreSQL and Redis)

### 1. Clone and Build

```bash
git clone https://github.com/SanathAishu/clinic-mgmt.git
cd clinic-mgmt
./mvnw clean package -DskipTests
```

### 2. Start All Services

```bash
./start-local.sh
```

This script will:
1. Start Docker containers (PostgreSQL, Redis)
2. Build services if needed
3. Start Eureka Server (service discovery)
4. Start API Gateway and all 8 microservices
5. Run Flyway migrations automatically
6. Display health check status

### 3. Verify Services

```bash
# Check Eureka dashboard
open http://localhost:8761

# Check API Gateway health
curl http://localhost:8080/actuator/health

# Test authentication
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hospital.com","password":"Admin123","name":"Admin","role":"ADMIN","gender":"MALE"}'
```

### 4. Stop All Services

```bash
./stop-local.sh

# To also stop infrastructure:
docker-compose down
```

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 8761 | Service discovery |
| API Gateway | 8080 | Single entry point |
| Auth Service | 8081 | JWT authentication |
| Patient Service | 8082 | Patient management |
| Doctor Service | 8083 | Doctor management |
| Appointment Service | 8084 | Appointment scheduling |
| Medical Records Service | 8085 | Medical records, prescriptions |
| Facility Service | 8086 | Room management |
| Notification Service | 8087 | Email notifications |
| Audit Service | 8088 | Audit logging |

## Project Structure

```
clinic-mgmt/
├── developer-docs/          # Developer documentation
├── docker/                  # Docker configurations
├── hospital-common/         # Shared library (DTOs, configs, enums)
├── eureka-server/           # Service discovery
├── api-gateway/             # API Gateway + JWT validation
├── auth-service/            # Authentication service
├── patient-service/         # Patient management
├── doctor-service/          # Doctor management
├── appointment-service/     # Appointment scheduling
├── medical-records-service/ # Medical records
├── facility-service/        # Room management
├── notification-service/    # Notifications
├── audit-service/           # Audit logging
├── start-local.sh           # Start all services
├── stop-local.sh            # Stop all services
└── docker-compose.yml       # Infrastructure
```

## Technology Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1, Spring WebFlux |
| Database | PostgreSQL 16 with R2DBC (reactive) |
| Caching | Redis 7 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Database Migrations | Flyway |
| Build Tool | Maven |
| Containerization | Docker, Docker Compose |

## Key Features

- **Reactive Architecture**: Non-blocking I/O with Spring WebFlux and R2DBC
- **Microservices**: 8 independent services with clear boundaries
- **Service Discovery**: Automatic service registration with Eureka
- **API Gateway**: Centralized routing and JWT validation
- **Database per Service**: Each service has its own PostgreSQL database
- **Flyway Migrations**: Version-controlled database schema
- **Distributed Caching**: Redis for cross-service caching
- **Audit Logging**: Comprehensive audit trail for compliance

## Development Commands

```bash
# Build all services
./mvnw clean package -DskipTests

# Build specific service
./mvnw clean package -pl patient-service -am

# Run tests
./mvnw test

# View service logs
tail -f logs/patient-service.log

# Check service health
curl http://localhost:8082/actuator/health
```

## Troubleshooting

### Services not starting

1. Check if infrastructure is running:
   ```bash
   docker-compose ps
   ```

2. Check service logs:
   ```bash
   tail -100 logs/<service-name>.log
   ```

3. Verify ports are free:
   ```bash
   lsof -i :8080
   ```

### Database connection issues

1. Verify PostgreSQL container:
   ```bash
   docker exec hospital-postgres psql -U postgres -c "\l"
   ```

2. Check R2DBC logs:
   ```bash
   grep -i "r2dbc\|connection" logs/<service-name>.log
   ```

## License

Proprietary - Hospital Management System
