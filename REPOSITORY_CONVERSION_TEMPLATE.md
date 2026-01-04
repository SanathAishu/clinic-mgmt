# Repository Conversion Template

## Quick Reference for Converting Remaining Repositories

### Step 1: Change Extends Clause
```java
// FROM:
public interface PatientRepository extends JpaRepository<Patient, UUID>

// TO:
public interface PatientRepository extends R2dbcRepository<Patient, UUID>
```

### Step 2: Update Imports
```java
// Remove:
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Add:
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
```

### Step 3: Convert Method Signatures

| JPA Method | R2DBC Equivalent |
|---|---|
| `Optional<T> findById(ID)` | `Mono<T> findById(ID)` |
| `Optional<T> findByX(Type x)` | `Mono<T> findByX(Type x)` |
| `List<T> findAll()` | `Flux<T> findAll()` |
| `List<T> findByX(Type x)` | `Flux<T> findByX(Type x)` |
| `Page<T> findByX(Type x, Pageable)` | `Flux<T> findByX(Type x)` |
| `boolean existsById(ID)` | `Mono<Boolean> existsById(ID)` |
| `long count()` | `Mono<Long> count()` |
| `long countByX(Type x)` | `Mono<Long> countByX(Type x)` |
| `void deleteById(ID)` | `Mono<Void> deleteById(ID)` |
| `S save(S entity)` | `Mono<S> save(S entity)` |

### Step 4: Convert @Query Annotations

**JPA (JPQL):**
```java
@Query("SELECT p FROM Patient p WHERE p.active = true")
Page<Patient> findActivePatients(Pageable pageable);
```

**R2DBC (Native SQL):**
```java
@Query("SELECT * FROM patients WHERE active = true")
Flux<Patient> findActivePatients();
```

### Step 5: Handle Pagination

**Important:** R2DBC doesn't have built-in pagination support like JPA. Options:

**Option A: Manual pagination**
```java
// Before:
Page<Patient> findByDisease(Disease disease, Pageable pageable);

// After (using LIMIT/OFFSET):
@Query("SELECT * FROM patients WHERE disease = :disease LIMIT :limit OFFSET :offset")
Flux<Patient> findByDisease(
    @Param("disease") Disease disease,
    @Param("limit") int limit,
    @Param("offset") int offset
);
```

**Option B: Get all and paginate in service**
```java
Flux<Patient> findByDisease(Disease disease);

// In service:
public Flux<Patient> findByDisease(Disease disease, int page, int size) {
    return repository.findByDisease(disease)
        .skip((long) page * size)
        .take(size);
}
```

### Step 6: Conversion Examples

#### Example 1: DoctorRepository
```java
// BEFORE
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Optional<Doctor> findByUserId(UUID userId);
    Optional<Doctor> findByEmail(String email);
    List<Doctor> findBySpecialty(Specialty specialty);
    Page<Doctor> findByAvailable(boolean available, Pageable pageable);
}

// AFTER
@Repository
public interface DoctorRepository extends R2dbcRepository<Doctor, UUID> {
    Mono<Doctor> findByUserId(UUID userId);
    Mono<Doctor> findByEmail(String email);
    Flux<Doctor> findBySpecialty(Specialty specialty);
    Flux<Doctor> findByAvailable(boolean available);
}
```

#### Example 2: AppointmentRepository
```java
// BEFORE
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByDoctorId(UUID doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.status = 'PENDING'")
    Page<Appointment> findPendingAppointments(Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate >= CURRENT_TIMESTAMP AND a.patientId = :patientId")
    List<Appointment> findUpcomingAppointments(@Param("patientId") UUID patientId);
}

// AFTER
@Repository
public interface AppointmentRepository extends R2dbcRepository<Appointment, UUID> {
    Flux<Appointment> findByPatientId(UUID patientId);
    Flux<Appointment> findByDoctorId(UUID doctorId);

    @Query("SELECT * FROM appointments WHERE status = 'PENDING'")
    Flux<Appointment> findPendingAppointments();

    @Query("SELECT * FROM appointments WHERE appointment_date >= CURRENT_TIMESTAMP AND patient_id = :patientId")
    Flux<Appointment> findUpcomingAppointments(@Param("patientId") UUID patientId);
}
```

#### Example 3: AuditLogRepository
```java
// BEFORE
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityId(UUID entityId);
    List<AuditLog> findByCategory(AuditCategory category);
    List<AuditLog> findByAction(AuditAction action);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}

// AFTER
@Repository
public interface AuditLogRepository extends R2dbcRepository<AuditLog, UUID> {
    Flux<AuditLog> findByEntityId(UUID entityId);
    Flux<AuditLog> findByCategory(AuditCategory category);
    Flux<AuditLog> findByAction(AuditAction action);

    @Query("SELECT * FROM audit_logs WHERE user_id = :userId ORDER BY timestamp DESC")
    Flux<AuditLog> findByUserId(@Param("userId") UUID userId);
}
```

### Batch Conversion Script

```bash
#!/bin/bash

# Convert all repositories in a service
convert_repos() {
    SERVICE=$1
    REPO_DIR="$SERVICE/src/main/java/com/hospital/*/repository"

    for file in $REPO_DIR/*.java; do
        # Replace extends JpaRepository with extends R2dbcRepository
        sed -i 's/extends JpaRepository</extends R2dbcRepository</g' "$file"

        # Replace imports
        sed -i 's/import org\.springframework\.data\.jpa\.repository\..*;/import org.springframework.data.r2dbc.repository.*;/g' "$file"

        # Replace Optional with Mono
        sed -i 's/Optional<\([^>]*\)>/Mono<\1>/g' "$file"

        # Replace List with Flux
        sed -i 's/List<\([^>]*\)>/Flux<\1>/g' "$file"

        # Replace Page with Flux
        sed -i 's/Page<\([^>]*\)>/Flux<\1>/g' "$file"

        # Add necessary imports
        if ! grep -q "import reactor.core.publisher.Mono;" "$file"; then
            sed -i '/import org\.springframework\.data\.r2dbc\.repository/a import reactor.core.publisher.Mono;' "$file"
        fi
        if ! grep -q "import reactor.core.publisher.Flux;" "$file"; then
            sed -i '/import org\.springframework\.data\.r2dbc\.repository/a import reactor.core.publisher.Flux;' "$file"
        fi

        echo "Converted: $file"
    done
}

# Usage:
# convert_repos "/home/sanath/Projects/Clinic_mgmt/patient-service"
```

### Critical Points

1. **Remove Pageable parameters** - R2DBC doesn't support pagination in queries
2. **Use native SQL** - @Query must use SQL, not JPQL
3. **Return reactive types** - Always return Mono or Flux
4. **Update services** - Services must handle Mono/Flux (use `.block()` in MVC if needed)
5. **Test carefully** - Reactive code behaves differently than synchronous
