# Microservices Migration Plan for Hospital Management System

## Overview
Convert the monolithic Hospital Management System into **8 domain-based microservices** using **Spring Cloud**, **Redis caching**, **PostgreSQL database with separate schemas**, **UUID primary keys**, and **hybrid REST/event-driven communication**.

## Target Architecture

### Microservices (8 Services)
1. **Auth Service** (Port 8081) - JWT generation (RS256), user management
2. **Patient Service** (Port 8082) - Patient demographics and CRUD
3. **Doctor Service** (Port 8083) - Doctor demographics and specialties
4. **Appointment Service** (Port 8084) - Scheduling with validation
5. **Medical Records Service** (Port 8085) - Records, prescriptions, reports
6. **Facility Service** (Port 8086) - Rooms and admissions (with Saga pattern)
7. **Notification Service** (Port 8087) - Event-driven email notifications
8. **Audit Service** (Port 8088) - HIPAA/GDPR compliance, security monitoring

### Infrastructure Components
- **Config Server** (Port 8888) - Centralized configuration
- **Eureka Server** (Port 8761) - Service discovery
- **API Gateway** (Port 8080) - Routing and JWT validation
- **RabbitMQ** (Ports 5672, 15672) - Event-driven messaging
- **Redis** (Port 6379) - Distributed caching
- **PostgreSQL** (Port 5432) - Primary database with JSONB support

### Database Strategy
- Single PostgreSQL instance with **8 separate schemas** (one per microservice)
- **UUID primary keys** - Distributed-friendly, no coordination needed
- **JSONB columns** - Flexible schema for metadata and audit logs
- Remove foreign key constraints across schemas
- Replace with application-level validation and events
- Use PostgreSQL's advanced features: full-text search, partial indexes, array types

## Key Technical Changes

### 1. Security: HS512 → RS256 Migration
**Current Problem:** Symmetric JWT (HS512) requires shared secret across all services

**Solution:**
- Auth Service generates RSA key pair (2048-bit)
- Auth Service holds **private key** (signs tokens)
- All services receive **public key** (validate tokens)
- Expose public key via `GET /api/auth/public-key`

**Files to Modify:**
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/config/security/jwt/JwtUtils.java`
- Create new Auth Service with RS256 implementation

### 2. Remove Cascade Relationships
**Current Problem:** CascadeType.ALL in Patient/Doctor entities cascade deletes across services

**Solution:** Remove all `@OneToMany` relationships pointing to other microservices

**Files to Modify:**
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/entity/Patient.java` (lines 75-85)
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/entity/Doctor.java`

**Changes:**
```java
// REMOVE these from Patient.java:
@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
private List<Appointment> appointments;
@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
private List<MedicalRecord> medicalRecords;
// etc.

// Replace cascade deletes with events:
// When Patient deleted → Publish PatientDeletedEvent
// Other services listen and clean up their data
```

### 3. Distributed Transactions: Saga Pattern
**Current Problem:** Room admission updates both Patient and Room atomically (lines 95-102 in RoomService.java)

**Solution:** Implement Choreography Saga with compensating transactions

**Files to Modify:**
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/service/RoomService/RoomService.java`

**Saga Flow:**
```
1. Facility Service: Reserve bed → Create RoomBooking (PENDING)
2. Publish PatientAdmissionRequestEvent
3. Patient Service: Update patient → Publish success/failure
4. Facility Service: Confirm booking OR compensate (release bed)
```

### 4. Replace Direct Service Calls with Feign Clients
**Current Problem:** Services directly access repositories across domains

**Solution:** Add Spring Cloud OpenFeign for inter-service REST calls

**Example - Appointment Service:**
```java
@FeignClient(name = "PATIENT-SERVICE")
public interface PatientServiceClient {
    @GetMapping("/api/patients/{id}")
    PatientDto getPatientById(@PathVariable Long id);
}
```

**Files to Modify:**
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/service/AppointmentService/AppointmentService.java`

### 5. Event-Driven Notifications
**Current Problem:** MailService called directly by all services

**Solution:** Extract to Notification Service, use RabbitMQ events

**Files to Modify:**
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/service/MailService/MailService.java`

**Event Flow:**
```
Appointment Service → AppointmentCreatedEvent → RabbitMQ → Notification Service → Email
```

### 6. Data Denormalization (Snapshots)
**Current Problem:** Cross-service joins cause N+1 queries

**Solution:** Appointment Service maintains local snapshots of Patient/Doctor data

**New Tables in Appointment Service:**
- `patient_snapshot` (id, name, email, disease)
- `doctor_snapshot` (id, name, email, specialty)

**Update Mechanism:**
- Patient/Doctor services publish update events
- Appointment Service consumes and updates snapshots

### 6.5. UUID Primary Keys Implementation

**Current Problem:**
- Auto-increment IDs require database coordination
- Sequential IDs expose business metrics (total patients, etc.)
- Difficult to merge data across services
- ID collision risk in distributed systems

**Solution:** Use UUID (Universally Unique Identifiers) for all primary keys

#### UUID Benefits for Microservices

1. **No Coordination:** Generate IDs without database round-trip
2. **Distributed-Friendly:** No collision risk across services
3. **Security:** Non-sequential, harder to guess/enumerate
4. **Offline Generation:** Create entities before database insert
5. **Data Migration:** Merge databases without ID conflicts
6. **Better for Replication:** No auto-increment sequence issues

#### PostgreSQL UUID Implementation

**Enable UUID Extension:**
```sql
-- Run once per database
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- For gen_random_uuid()
```

**Entity Example with UUID:**
```java
// Before (MySQL with Long ID)
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}

// After (PostgreSQL with UUID)
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    // Or use JPA's built-in UUID generator (recommended)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
```

**PostgreSQL Table Schema:**
```sql
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    age INTEGER,
    disease VARCHAR(100),
    blood_type VARCHAR(5),
    date_of_registration TIMESTAMP DEFAULT NOW(),
    gender VARCHAR(20),
    metadata JSONB DEFAULT '{}'::jsonb,  -- Flexible fields
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Index for common queries
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_disease ON patients(disease);
CREATE INDEX idx_patients_metadata ON patients USING GIN (metadata);

-- Full-text search index (for searching by name, address, etc.)
CREATE INDEX idx_patients_search ON patients
USING GIN (to_tsvector('english', name || ' ' || COALESCE(address, '')));
```

#### DTOs and Request Objects

```java
// DTOs use UUID instead of Long
@Data
@Builder
public class PatientDto {
    private UUID id;  // Changed from Long
    private String name;
    private String email;
    private Disease disease;
}

// Request objects
public class CreatePatientRequest {
    // No ID field - will be generated
    private String name;
    private String email;
}

public class UpdatePatientRequest {
    // ID passed as path variable, not in body
    private String name;
    private String email;
}
```

#### Service Layer Changes

```java
@Service
public class PatientService {

    public PatientDto getPatientById(UUID id) {  // Changed from Long
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + id));
        return convertToDto(patient);
    }

    public PatientDto createPatient(CreatePatientRequest request) {
        // ID generated automatically by Hibernate
        Patient patient = Patient.builder()
            .name(request.getName())
            .email(request.getEmail())
            // No need to set ID - Hibernate generates UUID
            .build();

        Patient saved = patientRepository.save(patient);

        // Publish event with UUID
        PatientCreatedEvent event = new PatientCreatedEvent(
            saved.getId(),  // UUID
            saved.getName(),
            saved.getEmail()
        );
        rabbitTemplate.convertAndSend("hospital.events.topic", "patient.created", event);

        return convertToDto(saved);
    }
}
```

#### REST API Changes

```java
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    // Path variable uses UUID
    @GetMapping("/{id}")
    public ResponseEntity<PatientDto> getPatient(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientDto> updatePatient(
        @PathVariable UUID id,
        @RequestBody UpdatePatientRequest request
    ) {
        return ResponseEntity.ok(patientService.updatePatient(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### Foreign Key References

```java
// Appointment entity referencing Patient and Doctor
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Store UUIDs as foreign keys (no @ManyToOne in microservices)
    @Column(name = "patient_id", nullable = false, columnDefinition = "UUID")
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false, columnDefinition = "UUID")
    private UUID doctorId;

    private LocalDateTime appointmentDate;
    private String reason;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;
}
```

#### Event Contracts with UUID

```java
// Events use UUID for entity references
@Data
@Builder
public class PatientCreatedEvent {
    private UUID id;           // Changed from Long
    private String name;
    private String email;
    private Disease disease;
    private LocalDateTime timestamp;
}

@Data
@Builder
public class AppointmentCreatedEvent {
    private UUID appointmentId;  // Changed from Long
    private UUID patientId;
    private UUID doctorId;
    private LocalDateTime appointmentDate;
}
```

#### Repository Changes

```java
// Repository uses UUID
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByEmail(String email);
    List<Patient> findByDisease(Disease disease);
}

// Custom queries with UUID
@Query("SELECT p FROM Patient p WHERE p.id IN :ids")
List<Patient> findByIds(@Param("ids") List<UUID> ids);
```

#### Migration from Long to UUID

**Step 1: Add UUID column alongside Long ID**
```sql
-- Add new UUID column
ALTER TABLE patients ADD COLUMN uuid_id UUID DEFAULT gen_random_uuid();

-- Populate UUIDs for existing rows
UPDATE patients SET uuid_id = gen_random_uuid() WHERE uuid_id IS NULL;

-- Make it NOT NULL
ALTER TABLE patients ALTER COLUMN uuid_id SET NOT NULL;
```

**Step 2: Update foreign keys**
```sql
-- Add UUID foreign key columns
ALTER TABLE appointments ADD COLUMN patient_uuid UUID;
ALTER TABLE appointments ADD COLUMN doctor_uuid UUID;

-- Copy references
UPDATE appointments a
SET patient_uuid = (SELECT p.uuid_id FROM patients p WHERE p.id = a.patient_id);

-- Drop old foreign keys and make UUID the primary
ALTER TABLE patients DROP CONSTRAINT patients_pkey;
ALTER TABLE patients ADD PRIMARY KEY (uuid_id);
ALTER TABLE patients DROP COLUMN id;
ALTER TABLE patients RENAME COLUMN uuid_id TO id;
```

**Step 3: Update application code**
- Change all `Long id` to `UUID id`
- Update DTOs, events, repositories
- Test thoroughly before production

#### UUID Performance Considerations

**Index Performance:**
- UUIDs are 128-bit (16 bytes) vs Long 64-bit (8 bytes)
- Slightly larger indexes, but negligible for modern hardware
- Use B-tree indexes (default in PostgreSQL)

**Best Practices:**
```sql
-- Use gen_random_uuid() (faster, cryptographically secure)
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid()
);

-- Avoid sequential UUIDs (use v4 random, not v1 timestamp)
-- Random UUIDs prevent index hotspots in distributed systems
```

**Benchmarks:**
- UUID vs BIGSERIAL insert performance: ~5% slower (negligible)
- Query performance: Nearly identical with proper indexes
- Storage: +8 bytes per row (acceptable tradeoff for benefits)

### 7. Distributed Caching Strategy

**Current Problem:**
- Repeated database queries for frequently accessed data
- Cross-service calls for patient/doctor lookups increase latency
- No caching layer for read-heavy operations

**Solution:** Implement Redis-based distributed caching

#### Redis Setup
```yaml
# docker-compose.yml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  command: redis-server --appendonly yes
  volumes:
    - redis-data:/data
```

#### Cache Strategy by Service

**Auth Service:**
- **Cache:** Public key (1 hour TTL)
- **Cache:** Validated JWT tokens → user details (token expiry TTL)
- **Pattern:** Cache-aside
```java
@Cacheable(value = "publicKey", key = "'rsa-public-key'")
public String getPublicKey() {
    return jwtUtils.getPublicKeyPEM();
}

@Cacheable(value = "users", key = "#email")
public UserDto getUserByEmail(String email) {
    return userRepository.findByEmail(email);
}
```

**Patient Service:**
- **Cache:** Patient by ID (15 min TTL)
- **Cache:** Patient search results (5 min TTL)
- **Invalidation:** On patient update/delete
```java
@Cacheable(value = "patients", key = "#id")
public PatientDto getPatientById(Long id) { }

@CacheEvict(value = "patients", key = "#id")
public PatientDto updatePatient(Long id, UpdatePatientRequest request) { }

@CacheEvict(value = "patients", allEntries = true)
public void clearPatientCache() { }
```

**Doctor Service:**
- **Cache:** Doctor by ID (15 min TTL)
- **Cache:** Doctors by specialty (10 min TTL)
- **Invalidation:** On doctor update/delete

**Appointment Service:**
- **Cache:** Appointment by ID (5 min TTL)
- **Cache:** Patient/Doctor snapshots (30 min TTL) - denormalized data
- **Invalidation:** On appointment update, on snapshot update events

**API Gateway:**
- **Cache:** Public key from Auth Service (1 hour TTL)
- **Cache:** Service routes from Eureka (5 min TTL)

#### Cache Invalidation Strategies

**Event-Driven Invalidation:**
```java
// Patient Service - publish cache invalidation event
@Transactional
public PatientDto updatePatient(Long id, UpdatePatientRequest request) {
    Patient updated = patientRepository.save(patient);

    // Evict local cache
    cacheManager.getCache("patients").evict(id);

    // Publish event for other instances
    rabbitTemplate.convertAndSend(
        "hospital.events.cache",
        "patient.cache.invalidate",
        new CacheInvalidationEvent("patients", id.toString())
    );

    return convertToDto(updated);
}

// Consumer in other Patient Service instances
@RabbitListener(queues = "patient.cache.invalidation")
public void handleCacheInvalidation(CacheInvalidationEvent event) {
    cacheManager.getCache(event.getCacheName()).evict(event.getKey());
}
```

**Time-based Expiration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("patients",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)));

        cacheConfigurations.put("doctors",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)));

        cacheConfigurations.put("publicKey",
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

#### Cache Dependencies

Add to each microservice `pom.xml`:
```xml
<!-- Spring Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Lettuce for async Redis -->
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
</dependency>

<!-- Spring Cache Abstraction -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

#### What NOT to Cache
- Medical Records, Prescriptions, Medical Reports (sensitive, must be real-time)
- Room booking status (real-time accuracy critical)
- Appointment creation/updates (write operations)
- Audit logs (write-only)

#### Cache Monitoring
```yaml
# Expose cache metrics
management:
  metrics:
    enable:
      cache: true
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,caches
```

### 8. Audit Logging & Compliance

**Requirements:**
- **HIPAA Compliance:** Track all access to protected health information (PHI)
- **GDPR Compliance:** Log data access, modifications, deletions
- **Security:** Track authentication attempts, authorization failures
- **Operations:** Track all CRUD operations on critical entities

#### Audit Log Architecture

**Option 1: Event-Driven Audit Service (Recommended)**

Create dedicated **Audit Service** (Port 8088) that consumes all domain events:

```
Patient Service → PatientCreatedEvent → RabbitMQ → Audit Service → MongoDB
Doctor Service → DoctorUpdatedEvent → RabbitMQ → Audit Service → MongoDB
Auth Service → LoginAttemptEvent → RabbitMQ → Audit Service → MongoDB
```

**Option 2: Database Triggers + Separate Audit Schema**

Each service writes to its own audit table within the same schema.

**Recommended: Option 1** - Centralized audit service with event sourcing

#### Audit Service Implementation

**Audit Event Schema (PostgreSQL with JSONB):**
```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType;        // "PATIENT_CREATED", "LOGIN_SUCCESS"
    private String entityType;       // "Patient", "Doctor", "Appointment"

    @Column(name = "entity_id", columnDefinition = "UUID")
    private UUID entityId;           // UUID of the entity

    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;             // User who performed action

    private String userName;
    private String userRole;         // "ADMIN", "USER"
    private String action;           // "CREATE", "UPDATE", "DELETE", "READ"
    private String serviceName;      // "patient-service"
    private LocalDateTime timestamp;
    private String ipAddress;

    // JSONB columns for flexible data storage
    @Column(name = "before_state", columnDefinition = "JSONB")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> before;  // State before change

    @Column(name = "after_state", columnDefinition = "JSONB")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> after;   // State after change

    @Column(name = "metadata", columnDefinition = "JSONB")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> metadata; // Additional context

    @Enumerated(EnumType.STRING)
    private AuditCategory category;  // AUTHENTICATION, DATA_ACCESS, DATA_MODIFICATION

    @Enumerated(EnumType.STRING)
    private SensitivityLevel sensitivity; // PUBLIC, INTERNAL, CONFIDENTIAL, PHI

    private boolean archived = false;
}

enum AuditCategory {
    AUTHENTICATION,      // Login, logout, token refresh
    AUTHORIZATION,       // Access denied, permission checks
    DATA_ACCESS,         // Read operations on PHI
    DATA_MODIFICATION,   // Create, Update, Delete
    SYSTEM_EVENT         // Service start/stop, errors
}

enum SensitivityLevel {
    PUBLIC,              // Non-sensitive data
    INTERNAL,            // Internal business data
    CONFIDENTIAL,        // Sensitive business data
    PHI                  // Protected Health Information
}
```

#### Audit Events to Track

**Authentication Events:**
```java
// Auth Service
public class LoginAttemptEvent {
    private String email;
    private boolean success;
    private String reason;          // "INVALID_PASSWORD", "USER_NOT_FOUND"
    private String ipAddress;
    private LocalDateTime timestamp;
}

public class LogoutEvent {
    private Long userId;
    private String email;
    private LocalDateTime timestamp;
}
```

**Data Access Events (HIPAA Required):**
```java
// Patient Service - track every patient record access
@Aspect
@Component
public class DataAccessAuditAspect {

    @AfterReturning(
        pointcut = "execution(* com.hospital..service.*Service.get*(..))",
        returning = "result"
    )
    public void auditDataAccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Get current user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        AuditEvent event = AuditEvent.builder()
            .eventType("DATA_ACCESS")
            .action("READ")
            .entityType(extractEntityType(result))
            .entityId(extractEntityId(result))
            .userId(auth.getName())
            .timestamp(LocalDateTime.now())
            .build();

        rabbitTemplate.convertAndSend("hospital.events.audit", "audit.data.access", event);
    }
}
```

**Data Modification Events:**
```java
// Patient Service
@Transactional
public PatientDto updatePatient(Long id, UpdatePatientRequest request) {
    Patient before = patientRepository.findById(id).orElseThrow();
    Patient after = applyUpdates(before, request);

    // Publish audit event with before/after state
    DataModificationEvent event = DataModificationEvent.builder()
        .eventType("PATIENT_UPDATED")
        .entityType("Patient")
        .entityId(id.toString())
        .userId(getCurrentUserId())
        .action("UPDATE")
        .before(convertToMap(before))
        .after(convertToMap(after))
        .timestamp(LocalDateTime.now())
        .sensitivity(SensitivityLevel.PHI)
        .build();

    rabbitTemplate.convertAndSend("hospital.events.audit", "audit.data.modification", event);

    return convertToDto(patientRepository.save(after));
}
```

**Critical Operations (Always Audit):**
- Patient/Doctor created, updated, deleted
- Medical records accessed or modified
- Prescriptions created or modified
- Login success/failure
- Authorization failures
- Appointment created/cancelled
- Room admissions/discharges
- Data exports (if implemented)

#### Audit Service Consumer

```java
@Service
public class AuditEventConsumer {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @RabbitListener(queues = "audit.all.events")
    public void handleAuditEvent(AuditEvent event) {
        AuditLog log = AuditLog.builder()
            .eventType(event.getEventType())
            .entityType(event.getEntityType())
            .entityId(event.getEntityId())
            .userId(event.getUserId())
            .action(event.getAction())
            .timestamp(event.getTimestamp())
            .ipAddress(event.getIpAddress())
            .before(event.getBefore())
            .after(event.getAfter())
            .category(determineCategory(event))
            .sensitivity(event.getSensitivity())
            .build();

        auditLogRepository.save(log);

        // If PHI access, also check for suspicious patterns
        if (log.getSensitivity() == SensitivityLevel.PHI) {
            anomalyDetectionService.checkForAnomalies(log);
        }
    }
}
```

#### Audit Query API

```java
// Audit Service REST endpoints
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAuthority('ADMIN')")
public class AuditController {

    // Get audit logs for specific entity
    @GetMapping("/entity/{entityType}/{entityId}")
    public List<AuditLogDto> getEntityAuditTrail(
        @PathVariable String entityType,
        @PathVariable String entityId
    ) {
        return auditService.getAuditTrail(entityType, entityId);
    }

    // Get user's activity history
    @GetMapping("/user/{userId}")
    public List<AuditLogDto> getUserActivity(@PathVariable Long userId) {
        return auditService.getUserActivity(userId);
    }

    // Search audit logs with filters
    @PostMapping("/search")
    public Page<AuditLogDto> searchAuditLogs(@RequestBody AuditSearchRequest request) {
        return auditService.search(request);
    }

    // Get failed login attempts
    @GetMapping("/security/failed-logins")
    public List<AuditLogDto> getFailedLogins(@RequestParam LocalDateTime since) {
        return auditService.getFailedLogins(since);
    }
}
```

#### Audit Storage - PostgreSQL with JSONB

**Why PostgreSQL for Audit Service:**
- **JSONB columns** - Flexible schema for before/after states
- **Write performance** - Optimized with unlogged tables for high throughput
- **Querying** - SQL queries for compliance reports
- **Indexing** - GIN indexes on JSONB for fast searches
- **Partitioning** - Table partitioning by date for performance
- **Single database** - No need for separate MongoDB instance

**PostgreSQL Audit Table with Optimizations:**
```sql
-- Main audit log table with partitioning
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    user_id UUID,
    user_name VARCHAR(255),
    user_role VARCHAR(50),
    action VARCHAR(20),  -- CREATE, UPDATE, DELETE, READ
    service_name VARCHAR(100),
    timestamp TIMESTAMP DEFAULT NOW() NOT NULL,
    ip_address VARCHAR(45),
    before_state JSONB,
    after_state JSONB,
    metadata JSONB,
    category VARCHAR(50),
    sensitivity VARCHAR(50),
    archived BOOLEAN DEFAULT FALSE
) PARTITION BY RANGE (timestamp);

-- Create monthly partitions
CREATE TABLE audit_logs_2025_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE audit_logs_2025_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- Create partitions for future months automatically (pg_partman extension)

-- Indexes for fast queries
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_category ON audit_logs(category);
CREATE INDEX idx_audit_logs_sensitivity ON audit_logs(sensitivity);

-- GIN indexes for JSONB columns
CREATE INDEX idx_audit_logs_before ON audit_logs USING GIN (before_state);
CREATE INDEX idx_audit_logs_after ON audit_logs USING GIN (after_state);
CREATE INDEX idx_audit_logs_metadata ON audit_logs USING GIN (metadata);

-- Composite index for PHI access queries
CREATE INDEX idx_audit_logs_phi_access ON audit_logs(sensitivity, timestamp)
    WHERE sensitivity = 'PHI';
```

**JSONB Converter for JPA:**
```java
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            return attribute == null ? null : objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Error converting Map to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : objectMapper.readValue(dbData, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to Map", e);
        }
    }
}
```

**Dependencies:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

#### Audit Retention Policy

```java
@Configuration
public class AuditRetentionConfig {

    // Automatically archive old audit logs
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    public void archiveOldAuditLogs() {
        LocalDateTime archiveBefore = LocalDateTime.now().minusYears(7);

        // HIPAA requires 6+ years retention
        List<AuditLog> oldLogs = auditLogRepository
            .findByTimestampBeforeAndArchived(archiveBefore, false);

        // Move to cold storage (S3, Glacier, etc.)
        archiveService.archive(oldLogs);

        // Mark as archived
        oldLogs.forEach(log -> log.setArchived(true));
        auditLogRepository.saveAll(oldLogs);
    }
}
```

#### Anomaly Detection (Security)

```java
@Service
public class AnomalyDetectionService {

    public void checkForAnomalies(AuditLog log) {
        // Detect suspicious patterns
        if (log.getCategory() == AuditCategory.DATA_ACCESS) {
            // Check for mass data access
            long accessCount = auditLogRepository.countByUserIdAndTimestampAfter(
                log.getUserId(),
                LocalDateTime.now().minusMinutes(10)
            );

            if (accessCount > 50) {
                // Alert: Possible data exfiltration
                alertService.sendSecurityAlert(
                    "Mass data access detected",
                    log.getUserId(),
                    accessCount
                );
            }
        }

        // Check for access outside business hours
        if (isOutsideBusinessHours(log.getTimestamp())) {
            alertService.sendSecurityAlert(
                "After-hours PHI access",
                log.getUserId(),
                log.getEntityId()
            );
        }
    }
}
```

#### Compliance Reports

```java
// Generate HIPAA compliance reports
@Service
public class ComplianceReportService {

    public HipaaComplianceReport generateMonthlyReport(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        return HipaaComplianceReport.builder()
            .totalPhiAccess(auditLogRepository.countBySensitivityAndTimestampBetween(
                SensitivityLevel.PHI, start, end))
            .uniqueUsersAccessedPhi(auditLogRepository.countDistinctUsersBySensitivity(
                SensitivityLevel.PHI, start, end))
            .failedLoginAttempts(auditLogRepository.countByEventTypeAndSuccess(
                "LOGIN_ATTEMPT", false, start, end))
            .unauthorizedAccessAttempts(auditLogRepository.countByCategory(
                AuditCategory.AUTHORIZATION, start, end))
            .dataModifications(auditLogRepository.countByCategory(
                AuditCategory.DATA_MODIFICATION, start, end))
            .build();
    }
}
```

#### RabbitMQ Queue for Audit Events

```yaml
# Add to RabbitMQ config
rabbitmq:
  queues:
    audit-all-events:
      name: audit.all.events
      durable: true
      binding:
        exchange: hospital.events.audit
        routing-key: "audit.#"  # All audit events
```

## Migration Phases (17 Weeks)

### Phase 1: Infrastructure Setup (Weeks 1-2)
**Deliverables:**
- Config Server, Eureka Server, API Gateway projects
- RabbitMQ Docker container
- Redis Docker container (distributed caching)
- PostgreSQL Docker container (primary database)
- hospital-common shared library (enums, events, DTOs with UUID)
- 8 PostgreSQL schemas created with UUID support

**Key Tasks:**
- Generate RSA key pair for JWT
- Setup Git repo for configurations
- Configure Eureka registration for all services
- Setup Redis configuration
- Setup PostgreSQL with UUID extensions (uuid-ossp, pgcrypto)
- Create 8 schemas in PostgreSQL
- Configure PostgreSQL connection pooling (HikariCP)
- Setup database migration tool (Flyway or Liquibase)
- Test service discovery end-to-end

### Phase 2: Extract Auth Service (Week 3)
**Deliverables:**
- Auth Service with RS256 JWT
- Centralized user table
- Public key distribution endpoint

**Key Tasks:**
- Migrate SecurityConfiguration and JwtUtils
- Implement RS256 token generation
- Create `GET /api/auth/public-key` endpoint
- Update Gateway to validate using public key

### Phase 3: Extract Patient & Doctor Services (Weeks 4-5)
**Deliverables:**
- Patient Service (CRUD, search, events)
- Doctor Service (CRUD, specialty search, events)

**Key Tasks:**
- Remove cascade relationships from entities
- Implement event publishing (PatientCreated, DoctorCreated, etc.)
- Register with Eureka
- Add Gateway routes

### Phase 4: Extract Appointment Service (Weeks 6-7)
**Deliverables:**
- Appointment Service with snapshot tables
- Feign clients for validation
- RabbitMQ consumers for snapshot updates

**Key Tasks:**
- Create PatientSnapshot and DoctorSnapshot entities
- Implement snapshot update consumers
- Migrate AppointmentReminderScheduler
- Test specialty matching validation

### Phase 5: Extract Medical Records Service (Weeks 8-9)
**Deliverables:**
- Medical Records Service (Records, Prescriptions, Reports)
- Event publishing for notifications

**Key Tasks:**
- Keep OneToOne relationships (within same schema)
- Replace Patient/Doctor foreign keys with IDs
- Implement validation via Feign calls
- Test CRUD operations

### Phase 6: Extract Facility Service (Weeks 10-11)
**Deliverables:**
- Facility Service with Saga implementation
- RoomBooking entity (new)

**Key Tasks:**
- Create RoomBooking table
- Implement admission Saga (request → confirm → compensate)
- Remove Patient.room field
- Test saga failure scenarios

### Phase 7: Extract Notification Service (Week 12)
**Deliverables:**
- Notification Service (event-driven only)
- All email templates migrated

**Key Tasks:**
- Create RabbitMQ consumers for all notification events
- Migrate email templates (HTML)
- Remove MailService from other services

### Phase 7.5: Implement Audit Service (Week 12-13)
**Deliverables:**
- Audit Service with PostgreSQL JSONB storage
- Audit event consumers for all services
- AOP aspects for automatic data access logging
- HIPAA compliance reporting API
- Table partitioning for audit logs

**Key Tasks:**
- Create Audit Service project
- Setup PostgreSQL connection with audit_service schema
- Create partitioned audit_logs table with JSONB columns
- Implement JSONB converter for JPA
- Implement RabbitMQ consumers for audit events
- Add AOP aspects to all services for automatic auditing
- Implement anomaly detection service
- Create compliance reporting endpoints
- Setup automated partition management
- Test HIPAA audit trail requirements

### Phase 8: Caching Implementation (Week 13)
**Deliverables:**
- Redis integration in all services
- Cache invalidation events
- Cache monitoring dashboards

**Key Tasks:**
- Add Redis dependencies to all services
- Implement @Cacheable annotations
- Setup cache invalidation event handlers
- Configure TTL for different cache types
- Add cache metrics to Prometheus
- Test cache hit/miss rates

### Phase 9: Testing & Refinement (Weeks 14-15)
**Deliverables:**
- Integration tests (TestContainers)
- End-to-end tests for critical flows
- Performance testing

**Key Tasks:**
- Test all Saga scenarios
- Load testing with JMeter
- Fix N+1 queries
- Document API contracts (OpenAPI)

### Phase 10: Production Deployment (Week 16-17)
**Deliverables:**
- Docker Compose for local development
- Kubernetes manifests (optional)
- CI/CD pipelines
- Monitoring (Prometheus, Grafana, Zipkin)

## Critical Files to Modify

### Entities (Remove Cascade Relationships)
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/entity/Patient.java` (lines 75-85)
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/entity/Doctor.java`
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/entity/MedicalRecord.java`

### Services (Add Feign Clients, Saga Pattern)
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/service/AppointmentService/AppointmentService.java`
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/service/RoomService/RoomService.java` (lines 77-126)
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/service/MailService/MailService.java`

### Security (RS256 Migration)
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/config/security/jwt/JwtUtils.java`
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/config/SecurityConfiguration.java`

### Schedulers (Migrate to Microservices)
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/notification/AppointmentReminderScheduler.java`
- `Hospital-Management-System/src/main/java/com/hospital/Hospital_Management_System/notification/RoomReminderScheduler.java`

## Shared Library (hospital-common)

Create shared library for cross-cutting concerns:

```
hospital-common/
├── enums/ (Disease, Specialty, Gender, Role, AppointmentStatus)
├── events/ (PatientCreatedEvent, AppointmentCreatedEvent, etc.)
├── dto/ (common DTOs)
├── exception/ (base exceptions)
├── config/ (RabbitMQConfig, FeignConfig)
└── util/ (DiseaseSpecialtyMapper, DateUtils)
```

## Spring Cloud Dependencies

Add to each microservice `pom.xml`:

```xml
<properties>
    <spring-cloud.version>2024.0.0</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- Config Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>

    <!-- OpenFeign -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>

    <!-- RabbitMQ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
</dependencies>
```

## RabbitMQ Exchanges and Queues

### Exchanges
- `hospital.events.topic` (topic exchange for notifications)
- `hospital.events.direct` (direct exchange for snapshots)
- `hospital.events.saga` (direct exchange for sagas)

### Queues
- `appointment.notifications` → Notification Service
- `prescription.notifications` → Notification Service
- `facility.notifications` → Notification Service
- `patient.updates` → Appointment Service (snapshots)
- `doctor.updates` → Appointment Service (snapshots)
- `patient.admission.request` → Patient Service (saga)
- `admission.success` → Facility Service (saga)
- `admission.failed` → Facility Service (saga)

## Database Schema SQL (PostgreSQL)

```sql
-- Enable UUID extension globally
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create all schemas (PostgreSQL uses schemas instead of databases)
CREATE SCHEMA IF NOT EXISTS auth_service;
CREATE SCHEMA IF NOT EXISTS patient_service;
CREATE SCHEMA IF NOT EXISTS doctor_service;
CREATE SCHEMA IF NOT EXISTS appointment_service;
CREATE SCHEMA IF NOT EXISTS medical_records_service;
CREATE SCHEMA IF NOT EXISTS facility_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS audit_service;

-- Create database user
CREATE USER hospital_user WITH PASSWORD 'your_secure_password';

-- Grant permissions on all schemas
GRANT ALL PRIVILEGES ON SCHEMA auth_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA patient_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA doctor_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA appointment_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA medical_records_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA facility_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA notification_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA audit_service TO hospital_user;

-- Grant table permissions (run after tables are created)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth_service TO hospital_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth_service TO hospital_user;
-- Repeat for other schemas...

-- Example: Patient table in patient_service schema
SET search_path TO patient_service;

CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    age INTEGER CHECK (age >= 0),
    password VARCHAR(255),
    address TEXT,
    disease VARCHAR(100),
    blood_type VARCHAR(5),
    date_of_registration TIMESTAMP DEFAULT NOW(),
    gender VARCHAR(20),

    -- Flexible metadata using JSONB
    metadata JSONB DEFAULT '{}'::jsonb,

    -- Audit fields
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

-- Indexes
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_disease ON patients(disease);
CREATE INDEX idx_patients_created_at ON patients(created_at DESC);
CREATE INDEX idx_patients_metadata ON patients USING GIN (metadata);

-- Full-text search
CREATE INDEX idx_patients_fulltext ON patients
USING GIN (to_tsvector('english', name || ' ' || COALESCE(address, '')));

-- Example: Appointment table with UUID foreign keys
SET search_path TO appointment_service;

CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,  -- No FK constraint (cross-schema)
    doctor_id UUID NOT NULL,   -- No FK constraint (cross-schema)
    appointment_date TIMESTAMP NOT NULL,
    reason TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',

    -- Denormalized data for performance
    patient_name VARCHAR(255),
    patient_email VARCHAR(255),
    doctor_name VARCHAR(255),
    doctor_specialty VARCHAR(100),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointments_date ON appointments(appointment_date);
CREATE INDEX idx_appointments_status ON appointments(status);

-- Composite index for common query
CREATE INDEX idx_appointments_patient_date ON appointments(patient_id, appointment_date DESC);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_patient_updated_at BEFORE UPDATE ON patient_service.patients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_appointment_updated_at BEFORE UPDATE ON appointment_service.appointments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Application Configuration (application.yml)

```yaml
# Patient Service
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hospital_db?currentSchema=patient_service
    username: hospital_user
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Use Flyway/Liquibase for migrations
    properties:
      hibernate:
        format_sql: true
        default_schema: patient_service
```

## REST API Endpoints (via API Gateway)

All requests go through API Gateway at `http://localhost:8080`

### Auth Service
- `POST /api/auth/register/patient`
- `POST /api/auth/register/doctor`
- `POST /api/auth/login`
- `GET /api/auth/public-key`

### Patient Service
- `GET/POST/PUT/DELETE /api/patients`
- `GET /api/patients/{id}`
- `GET /api/patients/search?name=&disease=`

### Doctor Service
- `GET/POST/PUT/DELETE /api/doctors`
- `GET /api/doctors/{id}`
- `GET /api/doctors/specialty/{specialty}`

### Appointment Service
- `GET/POST/PUT/DELETE /api/appointments`
- `GET /api/appointments/patient/{patientId}`
- `GET /api/appointments/doctor/{doctorId}`

### Medical Records Service
- `GET/POST/PUT/DELETE /api/medical-records`
- `GET/POST/PUT/DELETE /api/prescriptions`
- `GET/POST/PUT/DELETE /api/medical-reports`

### Facility Service
- `GET/POST/PUT/DELETE /api/rooms`
- `POST /api/rooms/{roomId}/admit`
- `POST /api/rooms/{roomId}/discharge/{patientId}`

## Event Contracts

### Patient Events
- `PatientCreatedEvent` (id, name, email, disease)
- `PatientUpdatedEvent` (id, name, email, disease)
- `PatientDeletedEvent` (id)

### Appointment Events
- `AppointmentCreatedEvent` (id, patientId, doctorId, date)
- `AppointmentReminderEvent` (appointmentId, patientEmail)

### Facility Events (Saga)
- `PatientAdmissionRequestEvent` (bookingId, patientId, roomId, dates)
- `AdmissionSuccessEvent` (bookingId, patientId)
- `AdmissionFailedEvent` (bookingId, patientId, reason)
- `PatientAdmittedEvent` (patientId, roomId) → Notification

## Monitoring & Observability

### Distributed Tracing
- Spring Cloud Sleuth + Zipkin
- Track requests across all microservices

### Metrics
- Spring Boot Actuator + Prometheus + Grafana
- Expose `/actuator/prometheus` endpoint

### Logging
- Centralized logging with ELK stack (Elasticsearch, Logstash, Kibana)

## Testing Strategy

1. **Unit Tests:** Service layer with mocked repositories
2. **Integration Tests:** TestContainers (MySQL + RabbitMQ)
3. **Contract Tests:** Spring Cloud Contract for API contracts
4. **E2E Tests:** Complete flows (registration → appointment → email)
5. **Saga Tests:** Test all compensation scenarios

## Deployment

### Local Development
```bash
# Start infrastructure
docker-compose up -d mysql rabbitmq

# Start services in order
./mvnw spring-boot:run -pl config-server
./mvnw spring-boot:run -pl eureka-server
./mvnw spring-boot:run -pl api-gateway
./mvnw spring-boot:run -pl auth-service
./mvnw spring-boot:run -pl patient-service
# ... etc
```

### Production (Kubernetes)
- Create Deployments, Services, Ingress for each microservice
- Use Helm charts for configuration management
- Implement health checks and readiness probes

## Success Criteria

- [ ] All 7 microservices running independently
- [ ] Services registered with Eureka
- [ ] API Gateway routing requests correctly
- [ ] JWT validation working with RS256
- [ ] Events flowing through RabbitMQ
- [ ] Saga pattern handling distributed transactions
- [ ] Email notifications working
- [ ] Zero data loss during migration
- [ ] All existing functionality preserved
- [ ] Performance equal or better than monolith

## Next Steps

1. Review and approve this plan
2. Setup Git repository structure for microservices
3. Begin Phase 1: Infrastructure setup
4. Execute phases sequentially with testing at each stage
5. Conduct code reviews and architecture validation
6. Deploy to staging environment for final testing
7. Production rollout with blue-green deployment
