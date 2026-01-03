# Hospital Common - Quarkus Reactive

Shared library module for Hospital Management System microservices. Contains centralized configurations, constants, and utilities to promote consistency and reduce duplication across all services.

## Module Structure

```
hospital-common-quarkus/
├── src/main/java/com/hospital/common/
│   ├── exception/              # Custom exception hierarchy
│   │   ├── BaseException.java  # Base class with HTTP status mapping
│   │   ├── NotFoundException.java
│   │   ├── ValidationException.java
│   │   ├── ConflictException.java
│   │   └── UnauthorizedException.java
│   │
│   ├── security/               # JWT token generation & validation
│   │   └── JwtService.java     # SmallRye JWT implementation
│   │
│   ├── cache/                  # Centralized cache key definitions
│   │   └── CacheKeys.java      # All Redis cache keys (constants)
│   │
│   ├── event/                  # Domain event definitions
│   │   ├── DomainEvent.java    # Base event class
│   │   ├── PatientEvents.java  # Patient service events
│   │   ├── DoctorEvents.java   # Doctor service events
│   │   ├── AppointmentEvents.java
│   │   ├── MedicalRecordEvents.java
│   │   ├── FacilityEvents.java
│   │   ├── AuthEvents.java
│   │   └── AuditEvents.java
│   │
│   ├── config/                 # Centralized configuration
│   │   └── RabbitMQConfig.java # All queues, exchanges, routing keys
│   │
│   └── constant/               # Other constants (enums, etc.)
│
└── src/main/resources/
    └── application.properties  # JWT & Cache TTL configurations
```

## Key Components

### 1. Exception Hierarchy

Centralized exception management with HTTP status code mapping:

```java
// Throws 404 Not Found
throw new NotFoundException("Patient not found");

// Throws 400 Bad Request
throw new ValidationException("email", "Invalid email format");

// Throws 409 Conflict
throw new ConflictException("Patient", "email", "user@example.com");

// Throws 401 Unauthorized
throw new UnauthorizedException("Invalid credentials");
```

**Usage in Controllers:**
```java
@Path("/api/patients/{id}")
public Uni<PatientDto> getPatient(@PathParam("id") UUID id) {
    return patientService.getById(id)
        .onItem().ifNull().failWith(() -> new NotFoundException("Patient", id.toString()));
}
```

### 2. JWT Service

Generates and manages JWT tokens using SmallRye JWT:

```java
@ApplicationScoped
public class AuthService {
    @Inject
    JwtService jwtService;

    public String login(User user) {
        return jwtService.generateToken(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
}
```

**Configuration (application.properties):**
```properties
jwt.issuer=hospital-system
jwt.secret=<your-secret-key>
jwt.expiration=86400  # 24 hours
```

**Token Claims:**
- `sub`: User ID (UUID)
- `email`: User email
- `name`: User name
- `role`: User role (PATIENT, DOCTOR, NURSE, RECEPTIONIST, ADMIN)
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp

### 3. Centralized Cache Keys

**Why centralized?**
- Single source of truth for cache key patterns
- Prevents typos and inconsistencies
- Easy to refactor cache keys
- Supports bulk invalidation

**Usage:**
```java
@CacheResult(cacheName = "patients")
public Uni<PatientDto> getPatientById(@CacheKey UUID id) {
    return repository.findById(id)
        .map(patient -> modelMapper.map(patient, PatientDto.class));
}

// Invalidate specific patient cache
@CacheInvalidate(cacheName = "patients")
public void deletePatient(@CacheKey UUID id) {
    // ...
}

// Using cache key builder
String key = CacheKeys.patientById(patientId.toString());
```

**Cache Keys Organization:**
- Patient: `patient:{id}`, `patient:email:{email}`, `patients:gender:{gender}`, `patients:list`
- Doctor: `doctor:{id}`, `doctor:email:{email}`, `doctors:specialty:{specialty}`
- Appointment: `appointment:{id}`, `appointments:patient:{patientId}`, `appointments:status:{status}`
- Medical Records: `medical-record:{id}`, `medical-records:patient:{patientId}`
- Snapshots (denormalization): `snapshot:patient:{id}`, `snapshot:doctor:{id}`
- Rooms/Bookings: `room:{id}`, `room-booking:{id}`
- Audit: `audit:logs:entity:{entityType}:{entityId}`

**Cache TTL Configuration (application.properties):**
```properties
quarkus.cache.caffeine.patients.expire-after-write=1H
quarkus.cache.caffeine.appointments.expire-after-write=15M
quarkus.cache.caffeine.patient-snapshots.expire-after-write=2H
```

### 4. RabbitMQ Configuration

Centralized event-driven architecture configuration:

**Exchanges:**
- `hospital.events.topic` - Broadcast notifications (pattern matching)
- `hospital.events.direct` - Service-to-service events
- `hospital.events.saga` - Saga pattern for distributed transactions

**Queues by Service:**

**Patient Service:**
- `patient.notifications` - Subscribe to patient events
- `patient.updates` - Snapshot updates for appointment service

**Doctor Service:**
- `doctor.notifications` - Subscribe to doctor events
- `doctor.updates` - Snapshot updates for appointment service

**Appointment Service:**
- `appointment.notifications` - Appointments created/cancelled

**Medical Records Service:**
- `medical.notifications` - Medical records, prescriptions, reports

**Facility Service (Saga Pattern):**
- `patient.admission.request` - Admission workflow initiation
- `admission.success` - Successful room booking
- `admission.failed` - Fallback on failure

**Notification Service:**
- `notification.appointments` - Send email on appointment events
- `notification.patients` - Send email on patient events
- `notification.medical` - Send email on medical record events

**Audit Service:**
- `audit.logs` - Log all create/update/delete operations

**Usage in Services:**

Publishing events:
```java
@Channel("patient-events")
Emitter<PatientCreatedEvent> eventEmitter;

public Uni<PatientDto> createPatient(PatientDto dto) {
    return repository.persist(patient)
        .call(p -> Uni.createFrom().completionStage(
            eventEmitter.send(new PatientCreatedEvent(
                "patient-service", correlationId,
                p.getId(), p.getName(), p.getEmail(), ...
            ))
        ));
}
```

Listening to events:
```java
@Incoming("patient-updates")
public Uni<Void> handlePatientUpdate(PatientUpdatedEvent event) {
    return snapshotService.updatePatientSnapshot(event);
}
```

Configuration (application.properties):
```properties
# Outgoing
mp.messaging.outgoing.patient-events.connector=smallrye-rabbitmq
mp.messaging.outgoing.patient-events.exchange.name=hospital.events.direct
mp.messaging.outgoing.patient-events.routing-key=patient.created

# Incoming
mp.messaging.incoming.patient-updates.connector=smallrye-rabbitmq
mp.messaging.incoming.patient-updates.queue.name=patient.updates
mp.messaging.incoming.patient-updates.exchange.name=hospital.events.direct
mp.messaging.incoming.patient-updates.routing-keys=patient.created,patient.updated
```

### 5. Domain Events

Base class for all domain events:

```java
@Data
public abstract class DomainEvent {
    private UUID eventId;           // Unique event identifier
    private LocalDateTime timestamp; // When event occurred
    private String source;          // Which service published it
    private String correlationId;   // For tracing related operations
}
```

**Event Types:**

**Patient Events:**
- `PatientCreatedEvent` - New patient registered
- `PatientUpdatedEvent` - Patient information changed
- `PatientDeletedEvent` - Patient record removed

**Doctor Events:**
- `DoctorCreatedEvent` - New doctor registered
- `DoctorUpdatedEvent` - Doctor information changed
- `DoctorDeletedEvent` - Doctor record removed

**Appointment Events:**
- `AppointmentCreatedEvent` - Appointment booked
- `AppointmentCancelledEvent` - Appointment cancelled
- `AppointmentCompletedEvent` - Appointment completed

**Medical Record Events:**
- `MedicalRecordCreatedEvent` - Record added
- `PrescriptionCreatedEvent` - Prescription issued
- `MedicalReportCreatedEvent` - Report generated

**Facility Events (Saga):**
- `PatientAdmissionRequestedEvent` - Admission requested
- `PatientAdmittedEvent` - Patient admitted (room booked)
- `PatientDischargedEvent` - Patient discharged
- `AdmissionFailedEvent` - Admission failed (saga compensation)

**Auth Events:**
- `UserRegisteredEvent` - New user registered
- `UserLoggedInEvent` - User logged in
- `PasswordChangedEvent` - Password changed

**Audit Events:**
- `AuditLogEvent` - Log of any action (create/update/delete)

## Usage in Child Services

### Step 1: Add Dependency
```xml
<dependency>
    <groupId>com.hospital</groupId>
    <artifactId>hospital-common-quarkus</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 2: Use Centralized Constants
```java
import com.hospital.common.cache.CacheKeys;
import com.hospital.common.config.RabbitMQConfig;
import com.hospital.common.exception.NotFoundException;
import com.hospital.common.security.JwtService;
import com.hospital.common.event.PatientEvents;

@ApplicationScoped
public class PatientService {
    @Inject
    JwtService jwtService;

    @Inject
    PatientRepository repository;

    @Channel(RabbitMQConfig.QueueConfig.PatientQueues.NOTIFICATIONS)
    Emitter<PatientEvents.PatientCreatedEvent> eventEmitter;

    @CacheResult(cacheName = CacheKeys.CACHE_PATIENTS)
    public Uni<Patient> findById(@CacheKey UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() ->
                new NotFoundException("Patient", id.toString())
            );
    }
}
```

### Step 3: Configure application.properties
```properties
# Use common configuration as base
mp.messaging.outgoing.patient-events.connector=smallrye-rabbitmq
mp.messaging.outgoing.patient-events.exchange.name=${hospital.events.direct}
mp.messaging.outgoing.patient-events.routing-key=${patient.created}
```

## Best Practices

1. **Use CacheKeys for all cache operations** - Never hardcode cache key strings
2. **Use RabbitMQConfig for all queue/exchange names** - Ensure consistency
3. **Use event classes from hospital-common-quarkus** - Single source of truth
4. **Always provide correlation IDs in events** - For distributed tracing
5. **Handle exceptions with proper HTTP status codes** - Use BaseException hierarchy
6. **Document custom exceptions** - Extend BaseException with meaningful messages

## Configuration Files

### application.properties
Located in `src/main/resources/application.properties`

Contains:
- JWT settings (issuer, secret, expiration)
- Cache TTL per cache name
- Logging configuration

**Override in child services:**
```properties
# In auth-service/application.properties
jwt.secret=${JWT_SECRET}  # From environment variable
jwt.expiration=86400
```

## Dependencies

- **Quarkus Core** - Framework
- **SmallRye JWT** - JWT token generation
- **Jakarta CDI** - Dependency injection
- **Jackson** - JSON serialization
- **Lombok** - Boilerplate reduction
- **MapStruct** - DTO mapping

## Testing

```java
@QuarkusTest
class JwtServiceTest {
    @Inject
    JwtService jwtService;

    @Test
    void testTokenGeneration() {
        String token = jwtService.generateToken(
            UUID.randomUUID(),
            "user@example.com",
            "John Doe",
            "PATIENT"
        );
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }
}
```

## Next Steps

1. Each service imports this module as a dependency
2. Services use CacheKeys, RabbitMQConfig, and event classes
3. Services extend exception classes for custom exceptions
4. Services use JwtService for token operations

## References

- Cache Keys: `src/main/java/com/hospital/common/cache/CacheKeys.java`
- RabbitMQ Config: `src/main/java/com/hospital/common/config/RabbitMQConfig.java`
- Event Classes: `src/main/java/com/hospital/common/event/*.java`
- Exceptions: `src/main/java/com/hospital/common/exception/*.java`
