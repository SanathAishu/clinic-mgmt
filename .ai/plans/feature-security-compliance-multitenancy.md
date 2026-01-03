# Security, Compliance & Multi-Tenancy - Architecture Plan

**Created:** 2026-01-03
**Status:** Planning / Decision Required
**Priority:** CRITICAL - Blocks all service migrations

## Executive Summary

This document analyzes critical architectural decisions for:
1. **Indian Healthcare Compliance** (DISHA / DPDPA)
2. **Multi-Tenancy** (Hospital/Clinic isolation)
3. **Access Control** (RBAC vs Policy-Based)

**These decisions must be made BEFORE migrating services** - they fundamentally affect:
- Database schema design
- JWT token structure
- Cache key patterns
- API design
- Frontend authorization logic

---

## 1. Indian Healthcare Compliance

### DISHA (Digital Information Security in Healthcare Act)

**Status:** Draft bill (not yet enacted as of 2024)

**Key Requirements (from draft):**
1. **Data Protection**
   - Encryption at rest and in transit
   - Audit trails for all access
   - Data retention policies
   - Right to erasure (patient data deletion)

2. **Consent Management**
   - Explicit patient consent for data sharing
   - Consent revocation mechanism
   - Consent audit logs

3. **Data Localization**
   - Health data must be stored in India
   - Cross-border transfer restrictions

4. **Breach Notification**
   - 72-hour breach notification requirement
   - Regulator notification mandatory

5. **Access Controls**
   - Role-based access to patient data
   - Purpose limitation (only access data needed for treatment)
   - Break-glass access for emergencies

### DPDPA 2023 (Digital Personal Data Protection Act)

**Status:** Enacted, rules pending

**Key Requirements:**
1. **Data Fiduciary Obligations**
   - Implement reasonable security safeguards
   - Data breach notification (timeline TBD)
   - Data retention limitation

2. **Data Principal Rights**
   - Right to access personal data
   - Right to correction
   - Right to erasure
   - Right to data portability

3. **Consent Requirements**
   - Clear, specific, informed, unconditional consent
   - Consent withdrawal mechanism
   - Purpose limitation

4. **Technical Requirements**
   - Anonymization and pseudonymization
   - Audit logs (who accessed what, when)
   - Data minimization

### Implementation Requirements for Compliance

| Requirement | Implementation in Quarkus |
|-------------|--------------------------|
| **Encryption at Rest** | PostgreSQL with encryption, encrypted backups |
| **Encryption in Transit** | HTTPS/TLS for all APIs, mTLS for inter-service |
| **Audit Logs** | Audit Service (already planned) - log all CRUD operations |
| **Consent Management** | New ConsentService + consent_records table |
| **Data Retention** | Scheduled job for data deletion after retention period |
| **Right to Erasure** | Soft delete + hard delete after grace period |
| **Access Controls** | Dynamic RBAC/ABAC (this document) |
| **Breach Detection** | Monitoring, alerting, automated notification |
| **Data Anonymization** | Masking service for non-healthcare staff |

---

## 2. Multi-Tenancy Analysis

### What is Multi-Tenancy?

**Scenario:** Multiple hospitals/clinics use the same application instance, but their data must be completely isolated.

**Example:**
- Hospital A (tenant_id: `hosp-001`) should NEVER see Hospital B's data
- Dr. Smith at Hospital A can't access patients from Hospital B
- Each tenant has independent admin controls

### Multi-Tenancy Approaches

#### Option 1: Database Per Tenant (Isolated DB)
```
hospital-a-db → patients_a, doctors_a, appointments_a
hospital-b-db → patients_b, doctors_b, appointments_b
```

**Pros:**
- Complete data isolation (highest security)
- Easy backup/restore per tenant
- Schema customization per tenant
- No query overhead

**Cons:**
- High infrastructure cost (many databases)
- Complex migrations (must migrate all tenant DBs)
- Difficult cross-tenant reporting
- Connection pool exhaustion

**Best for:** Enterprise customers, regulated industries, large tenants

---

#### Option 2: Schema Per Tenant (Same DB)
```
hospital_db
├── schema: hospital_a → patients, doctors, appointments
├── schema: hospital_b → patients, doctors, appointments
```

**Pros:**
- Better than single schema (data isolation)
- Lower cost than separate DBs
- Easier cross-tenant queries

**Cons:**
- Still high overhead (many schemas)
- Complex migrations
- Connection management issues
- PostgreSQL has schema limitations

**Best for:** Medium-sized tenants, moderate security requirements

---

#### Option 3: Discriminator Column (Shared Schema) ⭐ **RECOMMENDED**
```
patients table:
+------------+--------------+-------+
| tenant_id  | patient_id   | name  |
+------------+--------------+-------+
| hosp-001   | uuid-123     | John  |
| hosp-002   | uuid-456     | Jane  |
+------------+--------------+-------+

ALL queries: WHERE tenant_id = :currentTenantId
```

**Pros:**
- Lowest infrastructure cost
- Single migration for all tenants
- Easy cross-tenant analytics (with proper filters)
- Scales to thousands of tenants
- Best performance (single DB connection pool)

**Cons:**
- Risk of data leakage (if query forgets `tenant_id` filter)
- Cannot customize schema per tenant
- Shared resource contention

**Best for:** SaaS applications, small-medium clinics, cost-sensitive deployments

**Mitigation strategies:**
- Hibernate Filters (automatic `tenant_id` injection)
- Row-Level Security (PostgreSQL RLS)
- Panache entity listeners
- Comprehensive testing

---

### Multi-Tenancy in Quarkus Reactive

#### Database Schema Changes
```java
// BEFORE (single tenant)
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    private UUID id;
    private String name;
}

// AFTER (multi-tenant with discriminator)
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;  // Hospital/clinic identifier

    private String name;

    // Composite index for performance
    // CREATE INDEX idx_patients_tenant ON patients(tenant_id, id);
}
```

#### Automatic Tenant Filtering (Hibernate Filter)
```java
@Entity
@Table(name = "patients")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Patient {
    // ...
}

// In repository or service
@PrePersist
public void setTenant() {
    this.tenantId = TenantContext.getCurrentTenantId();
}
```

#### JWT Token Changes
```java
// BEFORE
{
  "sub": "user-id",
  "email": "doctor@hospital.com",
  "role": "DOCTOR"
}

// AFTER (with tenant)
{
  "sub": "user-id",
  "email": "doctor@hospital.com",
  "role": "DOCTOR",
  "tenant_id": "hosp-001",  // NEW
  "tenant_name": "City General Hospital"
}
```

#### Cache Key Changes
```java
// BEFORE
CacheKeys.patientById(patientId)  // "patient:uuid-123"

// AFTER (with tenant)
CacheKeys.patientById(tenantId, patientId)  // "hosp-001:patient:uuid-123"

// Implementation in CacheKeys.java
public static String patientById(String tenantId, String patientId) {
    return String.format("%s:patient:%s", tenantId, patientId);
}
```

#### RabbitMQ Event Changes
```java
// BEFORE
public class PatientCreatedEvent extends DomainEvent {
    private UUID patientId;
    private String name;
}

// AFTER (with tenant)
public class PatientCreatedEvent extends DomainEvent {
    private String tenantId;  // NEW
    private UUID patientId;
    private String name;
}

// Listeners must filter by tenant
@Incoming("patient-updates")
public Uni<Void> handlePatientUpdate(PatientUpdatedEvent event) {
    if (!event.getTenantId().equals(TenantContext.getCurrentTenantId())) {
        return Uni.createFrom().voidItem(); // Ignore other tenants
    }
    return snapshotService.updatePatientSnapshot(event);
}
```

---

## 3. Access Control: RBAC vs Policy-Based

### Option A: Role-Based Access Control (RBAC)

**How it works:**
- Users have roles (DOCTOR, NURSE, ADMIN, RECEPTIONIST)
- Roles have fixed permissions
- Access decisions: "Does user have role X?"

**Example:**
```java
public enum Role {
    PATIENT,
    DOCTOR,
    NURSE,
    RECEPTIONIST,
    ADMIN
}

// In controller
@RolesAllowed("DOCTOR")
@GET
@Path("/medical-records")
public Uni<List<MedicalRecordDto>> getAllRecords() {
    // Only doctors can access
}
```

**Pros:**
- Simple to implement
- Easy to understand
- Fast authorization checks
- Built-in Quarkus support (`@RolesAllowed`)

**Cons:**
- Inflexible (roles are static)
- No fine-grained control
- Can't express "doctor can only see their own patients"
- Role explosion (DOCTOR_CARDIOLOGY, DOCTOR_NEUROLOGY, etc.)

**Best for:** Simple applications, small teams, static permissions

---

### Option B: Policy-Based / Attribute-Based Access Control (ABAC)

**How it works:**
- Access based on attributes (user, resource, environment)
- Policies define complex rules
- Access decisions: "Does user meet policy conditions?"

**Example:**
```java
// Policy: Doctor can access medical records if:
// 1. They are treating the patient (assigned doctor)
// 2. OR they are in the same department
// 3. OR it's an emergency (break-glass access)

@Policy("canAccessMedicalRecord")
public boolean canAccessMedicalRecord(User user, MedicalRecord record) {
    return user.getId().equals(record.getDoctorId())
        || user.getDepartment().equals(record.getDepartment())
        || user.hasEmergencyAccess();
}
```

**Implementation with Open Policy Agent (OPA):**
```rego
# policy.rego
package hospital.authz

default allow = false

# Rule: Doctor can access their own patients' records
allow {
    input.user.role == "DOCTOR"
    input.resource.type == "medical_record"
    input.resource.doctor_id == input.user.id
}

# Rule: Admin can access everything in their tenant
allow {
    input.user.role == "ADMIN"
    input.user.tenant_id == input.resource.tenant_id
}

# Rule: Emergency access (break-glass)
allow {
    input.user.emergency_access == true
    input.resource.type == "medical_record"
}
```

**Pros:**
- Extremely flexible
- Fine-grained control
- Centralized policy management
- Can express complex rules (time-based, location-based, context-aware)
- Audit-friendly (policies are declarative)

**Cons:**
- More complex to implement
- Performance overhead (policy evaluation)
- Requires OPA or similar engine
- Steeper learning curve

**Best for:** Healthcare, finance, regulated industries, complex authorization requirements

---

### Option C: Dynamic RBAC (Hybrid Approach) ⭐ **RECOMMENDED**

**How it works:**
- Combine RBAC with dynamic permissions
- Roles as base, permissions as fine-grained control
- Access decisions: "Does user have permission X for resource Y?"

**Database Schema:**
```sql
-- Roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,  -- DOCTOR, NURSE, ADMIN, etc.
    description TEXT,
    UNIQUE(tenant_id, name)
);

-- Permissions table
CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,  -- medical_record:read, patient:write, etc.
    resource VARCHAR(50) NOT NULL,  -- medical_record, patient, appointment
    action VARCHAR(50) NOT NULL,    -- read, write, delete
    description TEXT,
    UNIQUE(name)
);

-- Role-Permission mapping (many-to-many)
CREATE TABLE role_permissions (
    role_id UUID REFERENCES roles(id),
    permission_id UUID REFERENCES permissions(id),
    PRIMARY KEY(role_id, permission_id)
);

-- User-Role mapping (users can have multiple roles)
CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role_id UUID REFERENCES roles(id),
    tenant_id VARCHAR(50) NOT NULL,
    PRIMARY KEY(user_id, role_id, tenant_id)
);

-- Dynamic permissions (resource-level)
CREATE TABLE user_resource_permissions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    resource_type VARCHAR(50) NOT NULL,  -- patient, medical_record
    resource_id UUID NOT NULL,           -- Specific patient ID, record ID
    permission VARCHAR(50) NOT NULL,     -- read, write, delete
    granted_by UUID REFERENCES users(id),
    granted_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    UNIQUE(user_id, resource_type, resource_id, permission)
);
```

**JWT Token Structure:**
```json
{
  "sub": "doctor-uuid-123",
  "email": "dr.smith@hospital.com",
  "name": "Dr. John Smith",
  "tenant_id": "hosp-001",
  "roles": ["DOCTOR", "DEPARTMENT_HEAD"],
  "permissions": [
    "medical_record:read",
    "medical_record:write",
    "patient:read",
    "appointment:read",
    "appointment:write"
  ],
  "department": "CARDIOLOGY"
}
```

**Backend Implementation:**
```java
// Permission check utility
@ApplicationScoped
public class PermissionService {

    @Inject
    JsonWebToken jwt;

    public boolean hasPermission(String permission) {
        List<String> userPermissions = jwt.getClaim("permissions");
        return userPermissions != null && userPermissions.contains(permission);
    }

    public boolean hasAnyPermission(String... permissions) {
        List<String> userPermissions = jwt.getClaim("permissions");
        return userPermissions != null &&
               Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    public boolean canAccessResource(String resourceType, UUID resourceId) {
        // Check dynamic permissions table
        return userResourcePermissionRepository
            .existsByUserIdAndResourceTypeAndResourceId(
                UUID.fromString(jwt.getSubject()),
                resourceType,
                resourceId
            )
            .await().indefinitely();
    }
}

// In controller
@GET
@Path("/medical-records/{id}")
public Uni<MedicalRecordDto> getMedicalRecord(@PathParam("id") UUID id) {
    return permissionService.canAccessResource("medical_record", id)
        .chain(hasAccess -> {
            if (!hasAccess) {
                return Uni.createFrom().failure(
                    new UnauthorizedException("No access to this medical record")
                );
            }
            return medicalRecordService.getById(id);
        });
}
```

**Frontend Impact:**
```typescript
// Frontend (React/Angular/Vue)
interface User {
  id: string;
  email: string;
  tenantId: string;
  roles: string[];
  permissions: string[];
  department: string;
}

// Permission utility
class PermissionService {
  private user: User;

  hasPermission(permission: string): boolean {
    return this.user.permissions.includes(permission);
  }

  hasRole(role: string): boolean {
    return this.user.roles.includes(role);
  }

  canAccessResource(resourceType: string, resourceId: string): Promise<boolean> {
    // Call backend API to check dynamic permissions
    return api.get(`/permissions/check/${resourceType}/${resourceId}`);
  }
}

// UI component example
function MedicalRecordButton({ recordId }: { recordId: string }) {
  const { user } = useAuth();
  const canEdit = user.permissions.includes('medical_record:write');

  if (!canEdit) {
    return null; // Hide button if no permission
  }

  return <button onClick={() => editRecord(recordId)}>Edit Record</button>;
}

// Route guard example
function ProtectedRoute({ permission, children }: { permission: string, children: React.ReactNode }) {
  const { user } = useAuth();

  if (!user.permissions.includes(permission)) {
    return <Navigate to="/unauthorized" />;
  }

  return <>{children}</>;
}

// Usage
<ProtectedRoute permission="medical_record:read">
  <MedicalRecordsList />
</ProtectedRoute>
```

---

## 4. Impact Analysis

### Backend Changes Required

| Component | RBAC | Dynamic RBAC | Policy-Based (OPA) |
|-----------|------|--------------|-------------------|
| **Database** | Add `role` enum | Add 4 tables (roles, permissions, mappings) | Add policy storage + OPA |
| **JWT Token** | Add `role` claim | Add `roles[]` + `permissions[]` | Add `attributes` object |
| **Authorization** | `@RolesAllowed` | Custom `@RequiresPermission` | OPA policy evaluation |
| **Cache Keys** | No change | Add permission cache | Add policy cache |
| **Performance** | Fast (1 check) | Medium (DB lookup for dynamic) | Slower (policy evaluation) |
| **Flexibility** | Low | High | Very High |
| **Complexity** | Low | Medium | High |

### Frontend Changes Required

| Aspect | RBAC | Dynamic RBAC | Policy-Based |
|--------|------|--------------|--------------|
| **Auth State** | Store `role` | Store `roles[]` + `permissions[]` | Store `user` + `policies` |
| **Permission Checks** | `if (role === 'DOCTOR')` | `if (permissions.includes('medical_record:read'))` | `await checkPolicy('canAccessRecord')` |
| **UI Components** | Role-based rendering | Permission-based rendering | Policy-based rendering |
| **Route Guards** | Simple role check | Permission array check | Async policy check |
| **API Calls** | No change | No change | Include context in request |
| **Complexity** | Low | Medium | High |

### Multi-Tenancy Impact

| Aspect | Impact | Implementation |
|--------|--------|----------------|
| **Database Queries** | ALL queries must filter by `tenant_id` | Hibernate Filters / Panache listeners |
| **JWT Token** | Add `tenant_id` claim | JwtService update |
| **Cache Keys** | Prefix all keys with `tenant_id` | CacheKeys refactor |
| **Events** | Add `tenantId` to all events | Event classes update |
| **API Calls** | Extract `tenant_id` from JWT | Request filter |
| **Testing** | Test with multiple tenants | Integration tests |

---

## 5. Recommendations

### For This Project (Healthcare SaaS)

#### ✅ **Multi-Tenancy: Discriminator Column (Shared Schema)**

**Reasoning:**
- Cost-effective for small-medium clinics
- Scales to thousands of tenants
- Easy to manage and migrate
- Adequate security with proper filters

**Mitigations:**
- PostgreSQL Row-Level Security (RLS)
- Hibernate Filters (automatic tenant_id injection)
- Comprehensive integration tests
- Regular security audits

#### ✅ **Access Control: Dynamic RBAC (Hybrid)**

**Reasoning:**
- Healthcare requires fine-grained control
- Dynamic permissions for specific patients/records
- Break-glass access for emergencies
- Audit trail for compliance
- Easier to implement than full ABAC
- Better than static RBAC

**Implementation:**
- Base roles: DOCTOR, NURSE, ADMIN, RECEPTIONIST, PATIENT
- Permissions: `resource:action` format (`medical_record:read`)
- Dynamic resource-level permissions for edge cases
- JWT includes roles + permissions for fast checks
- Database lookup for dynamic/resource-specific permissions

#### ✅ **Compliance: DPDPA + DISHA-Ready**

**Reasoning:**
- DPDPA is enacted (must comply)
- DISHA is draft but likely to pass
- Better to be compliant now than retrofit later

**Implementation:**
- Audit Service (already planned) - CRITICAL
- Consent Service (new) - HIGH
- Data retention policies - MEDIUM
- Right to erasure - MEDIUM
- Encryption (DB + Transit) - HIGH
- Breach detection/notification - MEDIUM

---

## 6. Implementation Roadmap

### Phase 0.5: Architecture Updates (BEFORE Service Migration)

**Week 1.5 (Insert before Auth Service migration):**

1. **Update hospital-common-quarkus** (2 days)
   - Add `TenantContext` utility
   - Add `@RequiresPermission` annotation
   - Add `PermissionService` utility
   - Update `DomainEvent` base class (add `tenantId`)
   - Update all event classes (add `tenantId` field)
   - Update `CacheKeys` (add `tenantId` parameter to all methods)
   - Update `JwtService` (include `tenantId`, `roles[]`, `permissions[]`)

2. **Create Permission/Role Schema** (1 day)
   - Create migration scripts
   - Create `Role`, `Permission`, `UserRole`, `UserResourcePermission` entities
   - Create repositories

3. **Create ConsentService** (1 day)
   - DPDPA compliance requirement
   - Patient consent management
   - Consent audit logs

4. **Update Templates** (1 day)
   - Update all migration templates with multi-tenancy
   - Add permission check examples
   - Update testing templates

### Phase 1: Auth Service Migration (Updated)

**Week 2:**
- Migrate Auth Service WITH multi-tenancy + dynamic RBAC
- Test tenant isolation
- Test permission checks
- Validate JWT token structure

### Phase 2+: Continue as Planned

All subsequent services use the updated templates with:
- Multi-tenancy built-in
- Dynamic RBAC checks
- Compliance-ready audit logs

---

## 7. Open Questions / Decisions Needed

### Critical Decisions (Block Migration)

1. **Confirm Multi-Tenancy Approach**
   - [ ] Discriminator column (RECOMMENDED)
   - [ ] Schema per tenant
   - [ ] Database per tenant

2. **Confirm Access Control Model**
   - [ ] Dynamic RBAC (RECOMMENDED)
   - [ ] Static RBAC
   - [ ] Full ABAC with OPA

3. **Compliance Priority**
   - [ ] DPDPA only (minimum)
   - [ ] DPDPA + DISHA-ready (RECOMMENDED)
   - [ ] Full HIPAA-equivalent

### Implementation Questions

1. **Tenant Onboarding**
   - How are new tenants created?
   - Self-service signup or admin-provisioned?
   - Tenant-specific customization needed?

2. **Permission Management**
   - Who can assign roles? (Tenant admin only?)
   - Who can grant resource-specific permissions?
   - Break-glass access approval workflow?

3. **Data Isolation Testing**
   - What level of testing for tenant isolation?
   - Penetration testing budget?
   - Third-party security audit?

4. **Frontend Framework**
   - React, Angular, Vue, or other?
   - Permission library preference?
   - UI component library for role-based rendering?

---

## 8. Estimated Impact

### Timeline Impact
- **Without these features:** 6 weeks
- **With Multi-Tenancy + Dynamic RBAC:** 8-9 weeks
- **With Multi-Tenancy + Dynamic RBAC + Full Compliance:** 10-12 weeks

### Complexity Impact
- **Code Complexity:** +30% (tenant filtering, permission checks)
- **Testing Complexity:** +50% (multi-tenant scenarios, permission combinations)
- **Infrastructure Complexity:** +20% (OPA if using policy-based, consent service)

### Cost Impact
- **Development Cost:** +30-50% time
- **Infrastructure Cost:** +10-20% (additional services, storage)
- **Ongoing Maintenance:** +20% (compliance updates, permission management)

---

## Next Steps

1. **User Decision Required:**
   - Confirm multi-tenancy approach
   - Confirm access control model
   - Confirm compliance scope

2. **Update Project Plan:**
   - Insert architecture update phase
   - Update service migration templates
   - Adjust timeline estimates

3. **Begin Implementation:**
   - Update hospital-common-quarkus
   - Create database schemas
   - Update JWT service
   - Migrate first service (Auth) with new architecture

---

**Decision Deadline:** Before starting Auth Service migration

**Impact:** Affects ALL services, JWT structure, database schema, frontend architecture

**Recommendation:** Schedule a planning session to finalize these decisions before proceeding.
