# Hospital Management System - Quarkus Reactive Migration

A reactive microservices migration from Spring Boot to Quarkus with native compilation support.

## Project Overview

This project migrates the clinic management system from Spring Boot to **Quarkus Reactive** with the following goals:

- **Reactive async** throughout (R2DBC, reactive RabbitMQ, reactive Redis)
- **Native compilation** for faster startup (1-2s) and smaller memory footprint (80-120MB per service)
- **Incremental migration** - services migrated one at a time, validated, then deployed
- **Business logic preservation** - minimal changes to domain models

## Project Structure

```
Clinic-Mgmt-Quarkus/
├── pom.xml                          # Parent POM (Quarkus BOM, plugins, profiles)
├── hospital-common-quarkus/         # Shared library (JWT, exceptions, configs)
├── auth-service/                    # Authentication service
├── patient-service/                 # Patient management
├── doctor-service/                  # Doctor management
├── appointment-service/             # Appointment scheduling
├── medical-records-service/         # Medical records management
├── facility-service/                # Room and bed management
├── notification-service/            # Event-driven notifications
├── audit-service/                   # HIPAA-compliant audit logging
├── api-gateway-quarkus/             # API gateway (Vert.x routing)
└── docker/                          # Infrastructure configs
```

## Build & Run

### Prerequisites

- **JDK 21+** (required for Quarkus 3.17+)
- **Maven 3.9+**
- **Docker** (for containerized builds and local development)
- **GraalVM/Oracle GraalVM** (optional, for native compilation)

### Build Profiles

#### 1. JVM Build (Default)
Fast compilation, suitable for development:
```bash
cd auth-service
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

#### 2. Native Build
Compile to native binary (3-5 minutes per service):
```bash
cd auth-service
./mvnw package -Pnative -DskipTests
./target/auth-service-1.0.0-runner
```

#### 3. Native Build with Docker
Build native image using Docker (no GraalVM required locally):
```bash
cd auth-service
./mvnw package -Pnative-docker -DskipTests
```

## Key Features

### Parent POM Configuration

The parent `pom.xml` provides:

- **Quarkus BOM**: Version 3.17.0 for all dependencies
- **Java 21 Configuration**: Modern language features enabled
- **Annotation Processors**: Lombok, MapStruct, Quarkus Panache
- **Build Profiles**: JVM (default), native, native-docker
- **Common Dependencies**: Testing, logging, CDI, Lombok

### Multi-Module Setup

Each service module:
1. Inherits from parent POM
2. Adds service-specific dependencies
3. Builds independently
4. Runs on dedicated ports (8081-8088)
5. Maintains separate PostgreSQL database

### Properties & Configuration

Key properties in parent POM:
```properties
quarkus.platform.version=3.17.0
maven.compiler.target=21
quarkus.package.type=jar          # Override with -Pnative
quarkus.native.container-build=true
```

## Migration Phases

### Phase 0: Infrastructure (Week 1) ✓
- [x] Parent POM with Quarkus BOM
- [ ] hospital-common-quarkus module
- [ ] API Gateway with Vert.x routing
- [ ] Docker Compose for local development

### Phase 1: Pilot Service (Week 2)
- [ ] Migrate Auth Service (pilot)
- [ ] Validate reactive patterns
- [ ] Test JWT flow

### Phase 2: Core Services (Weeks 3-4)
- [ ] Migrate Patient Service
- [ ] Migrate Doctor Service
- [ ] Migrate Appointment Service (snapshot pattern)

### Phase 3: Advanced Services (Week 5)
- [ ] Migrate Medical Records Service
- [ ] Migrate Facility Service (Saga pattern)
- [ ] Migrate Notification Service
- [ ] Migrate Audit Service

### Phase 4: Testing & Validation (Week 6)
- [ ] Integration testing
- [ ] Performance benchmarking
- [ ] Production deployment planning

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Quarkus | 3.17.0 |
| Language | Java | 21 |
| Reactive Library | Mutiny | (via Quarkus) |
| Database | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Messaging | RabbitMQ | 3.13 |
| ORM | Hibernate Reactive Panache | (via Quarkus) |
| REST | RESTEasy Reactive | (via Quarkus) |
| Security | SmallRye JWT | (via Quarkus) |
| Dependency Injection | Jakarta CDI | 4.0 |
| Build | Maven | 3.9+ |

## Common Commands

```bash
# Build all modules (JVM)
./mvnw clean package -DskipTests

# Build specific service (JVM)
cd auth-service && ./mvnw clean package

# Build specific service (native)
cd auth-service && ./mvnw package -Pnative -DskipTests

# Run dev mode (hot reload)
cd auth-service && ./mvnw quarkus:dev

# Run tests
./mvnw test

# Build with debugging
./mvnw clean package -Pnative -X

# Check native build details
./mvnw package -Pnative -Dquarkus.native.debug-symbols=true
```

## Development Workflow

### Working with a Single Service

```bash
# 1. Navigate to service
cd patient-service

# 2. Run in dev mode (auto-reload on code changes)
./mvnw quarkus:dev

# 3. Service runs at http://localhost:8082
# 4. DevUI available at http://localhost:8082/q/dev

# 5. Stop with Ctrl+C
```

### DevServices (Local Development)

Quarkus automatically starts PostgreSQL, RabbitMQ, and Redis containers during dev mode if configured. No manual setup needed!

## Configuration Files

Each service has `src/main/resources/application.properties`:

```properties
# Service Info
quarkus.application.name=patient-service
quarkus.http.port=8082

# PostgreSQL
quarkus.datasource.reactive.url=postgresql://localhost:5432/patient_service
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres

# RabbitMQ
quarkus.rabbitmq.host=localhost:5672
quarkus.rabbitmq.username=guest
quarkus.rabbitmq.password=guest

# Redis
quarkus.redis.hosts=redis://localhost:6379

# Hibernate/Database
quarkus.hibernate-orm.database.generation=update

# Build/Native
quarkus.package.type=jar
```

## Performance Expectations

### JVM Mode (Default)
- Startup: 2-4 seconds
- Memory: 150-200MB per service
- Throughput: Baseline (same as Spring Boot, but with better scalability under load)

### Native Mode (Production)
- Startup: 1-2 seconds (10x faster)
- Memory: 80-120MB per service (50-70% reduction)
- Throughput: 5-10x improvement under high concurrency

## Troubleshooting

### Native Build Failures
Most common issue: Reflection on classes not registered
```bash
# Add @RegisterForReflection to event classes
@RegisterForReflection
public class PatientCreatedEvent { ... }

# Or in application.properties
quarkus.reflection.register-for-reflection=com.hospital.auth.event.*
```

### RabbitMQ Connection Issues
Ensure DevServices is running or RabbitMQ is accessible:
```bash
docker run -it --rm -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```

### Database Schema Issues
Quarkus auto-creates schemas with `quarkus.hibernate-orm.database.generation=update`

For custom schema management:
```properties
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.sql-load-script=import.sql
```

## Migration Notes

### From Spring Boot to Quarkus

| Spring Boot | Quarkus Reactive |
|-------------|------------------|
| Spring Data JPA | Hibernate Reactive Panache |
| `Optional<T>` | `Uni<T>` |
| `List<T>` | `Multi<T>` |
| `@Service` | `@ApplicationScoped` |
| `@Autowired` | `@Inject` |
| `@RestController` | `@Path` (JAX-RS) |
| `RabbitTemplate` | `Emitter<T>` (SmallRye Messaging) |
| `@RabbitListener` | `@Incoming` (SmallRye Messaging) |
| `@Cacheable` | `@CacheResult` |
| Spring Security | SmallRye JWT |

## Next Steps

1. **Week 1, Day 1**: ✓ Create parent POM
2. **Week 1, Day 2**: Create hospital-common-quarkus module with JwtService
3. **Week 1, Days 3-5**: Create API Gateway
4. **Week 2**: Begin Auth Service migration

## References

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Hibernate Reactive Panache](https://quarkus.io/guides/hibernate-reactive-panache)
- [SmallRye Reactive Messaging](https://quarkus.io/guides/rabbitmq-reference)
- [SmallRye JWT](https://quarkus.io/guides/security-jwt)
- [Mutiny Patterns](https://smallrye.io/smallrye-mutiny/)

## License

Same as original clinic management system

## Migration Timeline

```
Week 1: Infrastructure Setup
├── Day 1: Parent POM ✓
├── Day 2: hospital-common-quarkus
├── Day 3-5: API Gateway

Week 2: Auth Service Pilot
├── Day 1-2: Repository & Service
├── Day 3: Controllers
├── Day 4: RabbitMQ Integration
├── Day 5: Native Build

Weeks 3-5: Core & Advanced Services
└── 1 service per 3-4 days

Week 6: Testing & Validation
└── Integration & Performance tests
```
