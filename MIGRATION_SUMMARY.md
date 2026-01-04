# RabbitMQ to REST APIs + R2DBC Migration Summary

## Completed Tasks ✓

### 1. Removed RabbitMQ Infrastructure
- ✓ Removed `spring-boot-starter-amqp` from all service pom.xml files
- ✓ Added `spring-boot-starter-data-r2dbc` and `r2dbc-postgresql` drivers
- ✓ Deleted RabbitMQ configuration classes:
  - `hospital-common/src/main/java/com/hospital/common/config/RabbitMQConfig.java`
  - `notification-service/src/main/java/com/hospital/notification/config/RabbitMQListenerConfig.java`
  - `audit-service/src/main/java/com/hospital/audit/config/RabbitMQListenerConfig.java`

### 2. Removed Event-Based Communication
- ✓ Deleted all event publisher classes:
  - `PatientEventPublisher.java`
  - `DoctorEventPublisher.java`
  - `AppointmentEventPublisher.java`
  - `MedicalRecordEventPublisher.java`
  - `FacilityEventPublisher.java`

- ✓ Deleted all event listener/consumer classes:
  - Notification service listeners (12 files)
  - Appointment service consumers (3 files)
  - Audit service listeners (5 files)

- ✓ Deleted all event classes from hospital-common (14 event DTOs)

### 3. Migrated to R2DBC
- ✓ Converted all JPA entities to R2DBC entities:
  - Changed imports from `jakarta.persistence` to `org.springframework.data.relational.core.mapping`
  - Removed `@Entity`, `@GeneratedValue`, `@EntityListeners`, `@Enumerated`, `@Transient` annotations
  - Simplified `@Column` annotations (removed attributes like nullable, unique, length, columnDefinition)
  - Updated `@Id` to use Spring Data annotation

**Entities converted:**
- patient-service: Patient
- doctor-service: Doctor
- appointment-service: Appointment, PatientSnapshot, DoctorSnapshot
- medical-records-service: MedicalRecord, Prescription, MedicalReport
- facility-service: Room, RoomBooking
- audit-service: AuditLog
- auth-service: User

- ✓ Started converting repositories from JpaRepository to R2dbcRepository:
  - PatientRepository updated (example)
  - Changed method signatures to return Mono/Flux for reactive support
  - Updated @Query methods to use native SQL instead of JPQL

## In Progress / TODO

### 1. Complete Repository Migration

**Repositories to convert:**
- `patient-service/PatientRepository.java` ✓ (example done)
- `doctor-service/DoctorRepository.java`
- `appointment-service/AppointmentRepository.java`
- `appointment-service/PatientSnapshotRepository.java`
- `appointment-service/DoctorSnapshotRepository.java`
- `medical-records-service/MedicalRecordRepository.java`
- `medical-records-service/PrescriptionRepository.java`
- `medical-records-service/MedicalReportRepository.java`
- `facility-service/RoomRepository.java`
- `facility-service/RoomBookingRepository.java`
- `audit-service/AuditLogRepository.java`
- `auth-service/UserRepository.java`

**Steps for each repository:**
```java
// Change from:
public interface PatientRepository extends JpaRepository<Patient, UUID> {
  Optional<Patient> findByUserId(UUID userId);
}

// Change to:
public interface PatientRepository extends R2dbcRepository<Patient, UUID> {
  Mono<Patient> findByUserId(UUID userId);
}
```

### 2. Create REST API Endpoints to Replace Events

**Example: Patient Service Publishing Snapshots**

Before (Event-based):
```
Patient Service publishes PatientCreatedEvent
→ Appointment Service listens and creates PatientSnapshot
```

After (REST API):
```java
// Appointment Service - Controller
@RestController
@RequestMapping("/internal/snapshots/patient")
public class PatientSnapshotController {

    @PostMapping
    public Mono<ResponseEntity<PatientSnapshot>> createPatientSnapshot(
            @RequestBody PatientSnapshotDTO dto) {
        return snapshotService.createOrUpdatePatientSnapshot(dto)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{patientId}")
    public Mono<ResponseEntity<PatientSnapshot>> updatePatientSnapshot(
            @PathVariable UUID patientId,
            @RequestBody PatientSnapshotDTO dto) {
        return snapshotService.createOrUpdatePatientSnapshot(dto)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{patientId}")
    public Mono<ResponseEntity<Void>> deletePatientSnapshot(
            @PathVariable UUID patientId) {
        return snapshotService.deletePatientSnapshot(patientId)
                .map(_ -> ResponseEntity.noContent().<Void>build());
    }
}
```

**Example: Patient Service Calling Appointment Service**

Before (Event-based):
```
Patient Service publishes event
→ Notification Service listens and sends email
```

After (REST API):
```java
// Appointment Service - Client (using RestTemplate or WebClient)
@Service
public class NotificationClient {
    private final WebClient webClient;

    public Mono<Void> notifyPatientCreated(PatientCreatedDTO dto) {
        return webClient.post()
                .uri("http://notification-service/api/notifications/patient-created")
                .bodyValue(dto)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}

// Patient Service - Service
@Service
public class PatientService {
    private final PatientRepository repository;
    private final NotificationClient notificationClient;

    public Mono<Patient> createPatient(CreatePatientDTO dto) {
        return repository.save(new Patient(...))
                .flatMap(patient ->
                    notificationClient.notifyPatientCreated(toDTO(patient))
                        .thenReturn(patient)
                );
    }
}
```

### 3. Update Application Properties for R2DBC

**Add to application.yml in each service:**

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/hospital_db
    username: postgres
    password: password
    pool:
      max-acquire-time: 2s
      max-create-connection-time: 2s
      initial-size: 10
      max-size: 20
      validation-query: SELECT 1
```

**Remove RabbitMQ configuration:**

```yaml
# REMOVE:
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        retry:
          enabled: true
```

### 4. Service Communication Migration Guide

| Former Pattern | New Pattern |
|---|---|
| Patient created event → Notification listens | POST to `/api/notifications/patient-created` |
| Patient updated event → Snapshots update | PUT to `/api/snapshots/patient/{id}` |
| Patient deleted event → Cache invalidate | DELETE to `/api/cache/invalidate` |
| Appointment created → Multiple listeners | POST to multiple endpoints (parallel) |

### 5. Important Configuration Notes

**R2DBC Connection String:**
```properties
# PostgreSQL R2DBC URL format
r2dbc:postgresql://[host]:[port]/[database]

# With credentials
r2dbc:postgresql://user:password@host:5432/database

# With SSL
r2dbc:postgresql://host:5432/database?sslMode=require
```

**Reactive to Synchronous Conversion (if needed for MVC controllers):**

```java
// Convert Mono to blocking (if needed in MVC controller)
@PostMapping("/patients")
public ResponseEntity<Patient> createPatient(@RequestBody CreatePatientDTO dto) {
    Patient patient = patientService.createPatient(dto)
            .block();  // Convert Mono to blocking
    return ResponseEntity.ok(patient);
}

// Better: Use Spring WebFlux for full async support
@PostMapping("/patients")
public Mono<ResponseEntity<Patient>> createPatient(@RequestBody CreatePatientDTO dto) {
    return patientService.createPatient(dto)
            .map(ResponseEntity::ok);
}
```

### 6. Database Migration

**Ensure all tables exist with proper schema.**

Since we removed JPA/Hibernate, ensure Flyway or Liquibase migration scripts are in place.

Example Flyway migration:
```sql
-- V1__Initial_schema.sql
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    gender VARCHAR(10),
    date_of_birth DATE,
    address VARCHAR(500),
    disease VARCHAR(50),
    medical_history TEXT,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Similar for other tables
```

## Next Steps

1. **Complete Repository Migration**: Convert all remaining repositories to R2DBC
2. **Implement REST Endpoints**: Create endpoints for all former event patterns
3. **Update Services**: Modify service classes to use repositories reactively
4. **Add WebClient/RestTemplate**: Configure HTTP clients for inter-service communication
5. **Update Controllers**: Make controllers reactive (WebFlux) or add async support
6. **Database Migration**: Ensure proper schema with Flyway/Liquibase
7. **Testing**: Update tests to work with R2DBC reactive repositories
8. **Configuration**: Add R2DBC pool configuration for each service
9. **Error Handling**: Implement retry logic for REST API calls (replace RabbitMQ resilience)
10. **Monitoring**: Add distributed tracing for REST API calls

## Benefits of This Migration

✓ **Simpler Architecture**: REST APIs are easier to understand and debug than message queues
✓ **Reactive**: R2DBC provides non-blocking database access
✓ **Scalability**: No need for RabbitMQ infrastructure
✓ **Synchronous Failures**: REST APIs fail fast; no message queue delays
✓ **Better Observability**: HTTP calls are easier to trace than async messages
✓ **Standards**: REST APIs are industry standard for microservices communication

## Potential Challenges

⚠ **Increased Coupling**: Services now depend directly on each other (REST calls)
⚠ **Error Handling**: Need to implement retry/circuit breaker patterns
⚠ **Transactions**: Distributed transactions now need saga pattern or choreography
⚠ **Performance**: Synchronous HTTP calls may be slower than fire-and-forget events
⚠ **Reactive Complexity**: R2DBC requires understanding of reactive streams (Mono/Flux)

## Rollback Plan

If issues arise:
1. Event publishers/consumers are deleted (not recoverable from git)
2. Keep RabbitMQ infrastructure running during transition
3. Gradually migrate services one at a time
4. Implement adapter layer for backward compatibility
