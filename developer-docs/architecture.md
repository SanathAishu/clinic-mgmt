# System Architecture

## Overview

The Hospital Management System is built using a microservices architecture with reactive programming patterns. Each service is independently deployable and communicates via REST APIs through a centralized API Gateway.

## Architecture Diagram

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
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │PostgreSQL│   │  Redis   │   │  Eureka  │
        │  :5432   │   │  :6379   │   │  :8761   │
        └──────────┘   └──────────┘   └──────────┘
```

## Services

### Infrastructure Services

| Service | Port | Description |
|---------|------|-------------|
| **Eureka Server** | 8761 | Service discovery and registration |
| **API Gateway** | 8080 | Single entry point, JWT validation, routing |

### Business Services

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **Auth Service** | 8081 | auth_service | User registration, JWT authentication (HS512) |
| **Patient Service** | 8082 | patient_service | Patient demographics, medical history |
| **Doctor Service** | 8083 | doctor_service | Doctor profiles, specialties, availability |
| **Appointment Service** | 8084 | appointment_service | Appointment scheduling, patient/doctor snapshots |
| **Medical Records Service** | 8085 | medical_records_service | Medical records, prescriptions, reports |
| **Facility Service** | 8086 | facility_service | Room management, room bookings |
| **Notification Service** | 8087 | - | Email notifications |
| **Audit Service** | 8088 | audit_service | Audit logging for compliance |

## Technology Stack

### Core Technologies

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.1 |
| Reactive Web | Spring WebFlux | 6.2.1 |
| Reactive DB | Spring Data R2DBC | 3.4.1 |
| Service Discovery | Spring Cloud Netflix Eureka | 2024.0.0 |
| API Gateway | Spring Cloud Gateway | 2024.0.0 |
| Database | PostgreSQL | 16 |
| Caching | Redis | 7 |
| Migrations | Flyway | 10.x |

### Reactive Stack

The system uses a fully reactive stack for non-blocking I/O:

```
┌─────────────────────────────────────────────────────────────┐
│                     Reactive Stack                          │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer    │  @RestController with Mono/Flux      │
├─────────────────────────────────────────────────────────────┤
│  Service Layer       │  Reactive business logic             │
├─────────────────────────────────────────────────────────────┤
│  Repository Layer    │  R2dbcRepository<Entity, UUID>       │
├─────────────────────────────────────────────────────────────┤
│  Database Driver     │  r2dbc-postgresql                    │
└─────────────────────────────────────────────────────────────┘
```

## Database Architecture

### Database Per Service

Each microservice has its own dedicated PostgreSQL database:

| Service | Database | Tables |
|---------|----------|--------|
| auth-service | auth_service | users |
| patient-service | patient_service | patients |
| doctor-service | doctor_service | doctors |
| appointment-service | appointment_service | appointments, patient_snapshots, doctor_snapshots |
| medical-records-service | medical_records_service | medical_records, prescriptions, medical_reports |
| facility-service | facility_service | rooms, room_bookings |
| audit-service | audit_service | audit_logs |

### R2DBC Configuration

Each service uses R2DBC for reactive database access:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/{database}
    username: postgres
    password: postgres
    pool:
      initial-size: 10
      max-size: 20
```

### Entity Pattern

Entities implement `Persistable<UUID>` for R2DBC compatibility:

```java
@Table("patients")
@Data
@Builder
public class Patient implements Persistable<UUID> {
    @Id
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    // ... other fields

    @Override
    public boolean isNew() {
        return isNew;
    }
}
```

## Authentication & Security

### JWT Authentication (HS512)

```
┌──────────┐     ┌─────────────┐     ┌──────────────┐
│  Client  │────►│ API Gateway │────►│ Auth Service │
└──────────┘     └─────────────┘     └──────────────┘
     │                  │
     │  1. Login        │
     │─────────────────►│
     │                  │
     │  2. JWT Token    │
     │◄─────────────────│
     │                  │
     │  3. Request +    │
     │     Bearer Token │
     │─────────────────►│
     │                  │
     │  4. Validate JWT │
     │                  │──────────────────────────►│
     │                  │     Forward if valid      │
     │                  │◄──────────────────────────│
     │  5. Response     │
     │◄─────────────────│
```

### Roles

| Role | Description |
|------|-------------|
| ADMIN | Full system access |
| DOCTOR | Access to patients, appointments, medical records |
| NURSE | Limited access to patient info and appointments |
| RECEPTIONIST | Appointment scheduling, basic patient info |
| PATIENT | Own records and appointments only |

## Inter-Service Communication

### WebClient with Load Balancing

Services communicate via reactive WebClient with Eureka-based load balancing:

```java
@Configuration
public class WebClientConfig {
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}

// Usage
webClient.get()
    .uri("http://patient-service/api/patients/{id}", patientId)
    .retrieve()
    .bodyToMono(Patient.class);
```

### Snapshot Pattern

The Appointment Service maintains local snapshots of patient and doctor data to reduce cross-service calls:

```
┌──────────────────┐     ┌──────────────────────────┐
│  Patient Service │     │   Appointment Service    │
├──────────────────┤     ├──────────────────────────┤
│     patients     │────►│    patient_snapshots     │
└──────────────────┘     │    doctor_snapshots      │
                         │    appointments          │
┌──────────────────┐     └──────────────────────────┘
│  Doctor Service  │                ▲
├──────────────────┤                │
│     doctors      │────────────────┘
└──────────────────┘
```

## Caching Strategy

### Redis Caching

Services use Redis for distributed caching with cache-aside pattern:

```java
@Cacheable(value = "patients", key = "#id")
public Mono<Patient> getPatient(UUID id) {
    return patientRepository.findById(id);
}

@CacheEvict(value = "patients", key = "#id")
public Mono<Patient> updatePatient(UUID id, UpdatePatientRequest request) {
    // ...
}
```

## Flyway Migrations

Database schemas are version-controlled with Flyway:

```
src/main/resources/db/migration/
├── V1__Initial_schema.sql    # Creates tables and indexes
├── V2__Add_columns.sql       # Schema updates
└── V3__Add_indexes.sql       # Performance indexes
```

Migrations run automatically on service startup.

## Shared Library (hospital-common)

Common code shared across all services:

```
hospital-common/
├── config/
│   ├── JacksonConfig.java       # JSON serialization
│   ├── WebClientConfig.java     # Load-balanced WebClient
│   └── WebFluxConfig.java       # WebFlux codecs
├── dto/
│   ├── ApiResponse.java         # Standard response wrapper
│   └── ErrorResponse.java       # Error response format
├── enums/
│   ├── Disease.java             # 24 disease types
│   ├── Specialty.java           # Medical specialties
│   ├── Gender.java              # Gender options
│   └── Role.java                # User roles
├── exception/
│   └── GlobalExceptionHandler.java
└── security/
    └── JwtUtils.java            # JWT utilities
```

## Deployment

### Local Development

```bash
# Start infrastructure
docker-compose up -d

# Start all services
./start-local.sh

# Stop all services
./stop-local.sh
```

### Docker Compose Infrastructure

```yaml
services:
  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    volumes:
      - ./docker/postgres/init-databases.sh:/docker-entrypoint-initdb.d/init.sh

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

## Monitoring & Observability

### Actuator Endpoints

All services expose Spring Boot Actuator endpoints:

```
GET /actuator/health    # Health check
GET /actuator/info      # Service info
GET /actuator/metrics   # Metrics
```

### Logging

Services log to `./logs/<service-name>.log`:

```bash
# View logs
tail -f logs/patient-service.log

# Search for errors
grep -i error logs/*.log
```

## Design Decisions

### Why Reactive?

- **Non-blocking I/O**: Better resource utilization under high concurrency
- **Backpressure**: Handles slow consumers gracefully
- **Scalability**: More requests per thread compared to blocking I/O

### Why Database Per Service?

- **Loose coupling**: Services can evolve independently
- **Data ownership**: Each service owns its data
- **Technology freedom**: Can use different databases per service

### Why Snapshots?

- **Reduced latency**: Avoids cross-service calls for common data
- **Fault tolerance**: Service remains functional if dependencies are down
- **Eventual consistency**: Acceptable for read-heavy operations
