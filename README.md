# Hospital Management System

A comprehensive hospital management system built with Spring Boot microservices, featuring reactive programming with **Spring WebFlux** and **R2DBC**.

## Quick Start

```bash
# Clone and build
git clone https://github.com/SanathAishu/clinic-mgmt.git
cd clinic-mgmt
./mvnw clean package -DskipTests

# Start all services
./start-local.sh

# Stop all services
./stop-local.sh
```

## Architecture

```
┌─────────────────┐
│   API Gateway   │ :8080
└────────┬────────┘
         │
    ┌────┴────┬────────┬────────┬────────┬────────┬────────┬────────┐
    │         │        │        │        │        │        │        │
┌───▼───┐ ┌───▼───┐ ┌──▼──┐ ┌───▼───┐ ┌──▼───┐ ┌──▼───┐ ┌──▼───┐ ┌──▼──┐
│ Auth  │ │Patient│ │Doctor│ │Appoint│ │Medical│ │Facil.│ │Notif.│ │Audit│
│ :8081 │ │ :8082 │ │:8083 │ │ :8084 │ │ :8085 │ │:8086 │ │:8087 │ │:8088│
└───────┘ └───────┘ └─────┘ └───────┘ └──────┘ └──────┘ └──────┘ └─────┘
    │         │        │        │        │        │        │        │
    └─────────┴────────┴────────┴────────┴────────┴────────┴────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
        ┌─────▼─────┐        ┌─────▼─────┐        ┌─────▼─────┐
        │ PostgreSQL│        │   Redis   │        │  Eureka   │
        │   :5432   │        │   :6379   │        │   :8761   │
        └───────────┘        └───────────┘        └───────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Single entry point, JWT validation |
| Auth Service | 8081 | User registration, JWT authentication |
| Patient Service | 8082 | Patient demographics |
| Doctor Service | 8083 | Doctor profiles, specialties |
| Appointment Service | 8084 | Appointment scheduling |
| Medical Records | 8085 | Medical records, prescriptions |
| Facility Service | 8086 | Room management |
| Notification Service | 8087 | Email notifications |
| Audit Service | 8088 | Audit logging |

## Technology Stack

- **Java 21** + **Spring Boot 3.4.1**
- **Spring WebFlux** (reactive web)
- **R2DBC** (reactive database access)
- **PostgreSQL 16** (database per service)
- **Redis 7** (distributed caching)
- **Spring Cloud** (Eureka, Gateway)
- **Flyway** (database migrations)

## Documentation

Detailed documentation is available in the [developer-docs](./developer-docs/) directory:

| Document | Description |
|----------|-------------|
| [Developer Guide](./developer-docs/README.md) | Getting started, project structure |
| [Architecture](./developer-docs/architecture.md) | System design, tech stack details |
| [Database Setup](./developer-docs/database-setup.md) | PostgreSQL, Flyway, R2DBC |
| [API Reference](./developer-docs/api-reference.md) | REST endpoints, examples |

## API Examples

```bash
# Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hospital.com","password":"Admin123","name":"Admin","role":"ADMIN","gender":"MALE"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hospital.com","password":"Admin123"}'

# Use token for authenticated requests
curl http://localhost:8080/api/patients \
  -H "Authorization: Bearer {token}"
```

## Development

```bash
# Build specific service
./mvnw clean package -pl patient-service -am

# Run tests
./mvnw test

# View logs
tail -f logs/patient-service.log
```

## License

Proprietary - Hospital Management System
