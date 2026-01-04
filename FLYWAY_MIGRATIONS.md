# Flyway Database Migrations

## ✅ Migration Setup Complete

All 7 services now have Flyway migrations configured and ready to run.

### Migration Files Created

| Service | Tables | Migration File | Key Features |
|---------|--------|----------------|--------------|
| **auth-service** | 1 | V1__Initial_schema.sql | `users` - Authentication & authorization |
| **patient-service** | 1 | V1__Initial_schema.sql | `patients` - Demographics & medical history |
| **doctor-service** | 1 | V1__Initial_schema.sql | `doctors` - Professional info & specialties |
| **appointment-service** | 3 | V1__Initial_schema.sql | `appointments`, `patient_snapshots`, `doctor_snapshots` |
| **medical-records-service** | 3 | V1__Initial_schema.sql | `medical_records`, `prescriptions`, `medical_reports` |
| **facility-service** | 2 | V1__Initial_schema.sql | `rooms`, `room_bookings` |
| **audit-service** | 1 | V1__Initial_schema.sql | `audit_logs` - Comprehensive audit trail |

**Total:** 12 tables with 70+ indexes

## Database Schemas

### 1. auth-service Database: `auth_service`

**Table: `users`**
```sql
- id (UUID, PK)
- email (VARCHAR, UNIQUE)
- password (VARCHAR) -- bcrypt hashed
- name (VARCHAR)
- role (VARCHAR) -- ADMIN, DOCTOR, NURSE, RECEPTIONIST, PATIENT
- active (BOOLEAN)
- created_at, updated_at, last_login (TIMESTAMP)

Indexes: email, role, active
```

### 2. patient-service Database: `patient_service`

**Table: `patients`**
```sql
- id (UUID, PK)
- user_id (UUID, UNIQUE)
- name, email (UNIQUE), phone
- gender, date_of_birth
- address, disease
- medical_history (TEXT)
- emergency_contact, emergency_phone
- active (BOOLEAN)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: user_id, email, disease, active, created_at
```

### 3. doctor-service Database: `doctor_service`

**Table: `doctors`**
```sql
- id (UUID, PK)
- user_id (UUID, UNIQUE)
- name, email (UNIQUE), phone
- gender, specialty
- license_number, years_of_experience
- qualifications, biography
- clinic_address, consultation_fee
- available, active (BOOLEAN)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: user_id, email, specialty, license_number, available, active, created_at
```

### 4. appointment-service Database: `appointment_service`

**Tables:**

**4.1 `appointments`**
```sql
- id (UUID, PK)
- patient_id, doctor_id (UUID) -- No FK constraints
- appointment_date (TIMESTAMP)
- status (VARCHAR) -- PENDING, CONFIRMED, CANCELLED, COMPLETED
- reason, notes (TEXT)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: patient_id, doctor_id, appointment_date, status, created_at
```

**4.2 `patient_snapshots`** (Denormalized)
```sql
- patient_id (UUID, PK)
- name, email, phone
- gender, disease
- last_updated (TIMESTAMP)

Indexes: email
```

**4.3 `doctor_snapshots`** (Denormalized)
```sql
- doctor_id (UUID, PK)
- name, email, phone
- gender, specialty
- last_updated (TIMESTAMP)

Indexes: email, specialty
```

### 5. medical-records-service Database: `medical_records_service`

**Tables:**

**5.1 `medical_records`**
```sql
- id (UUID, PK)
- patient_id, doctor_id (UUID)
- record_date (DATE)
- diagnosis, symptoms, treatment (TEXT)
- notes (TEXT)
- active (BOOLEAN)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: patient_id, doctor_id, record_date, active
```

**5.2 `prescriptions`**
```sql
- id (UUID, PK)
- patient_id, doctor_id, medical_record_id (UUID)
- prescription_date (DATE)
- medication_name, dosage, frequency, duration
- instructions (TEXT)
- refillable (BOOLEAN), refills_remaining (INTEGER)
- active (BOOLEAN)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: patient_id, doctor_id, medical_record_id, prescription_date, active, refillable
```

**5.3 `medical_reports`**
```sql
- id (UUID, PK)
- patient_id, doctor_id, medical_record_id (UUID)
- report_type (VARCHAR) -- LAB, XRAY, MRI, CT, etc.
- report_date (DATE)
- report_title, report_content (TEXT)
- file_url (VARCHAR)
- active (BOOLEAN)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: patient_id, doctor_id, medical_record_id, report_type, report_date, active
```

### 6. facility-service Database: `facility_service`

**Tables:**

**6.1 `rooms`**
```sql
- id (UUID, PK)
- room_number (VARCHAR, UNIQUE)
- room_type (VARCHAR) -- GENERAL, PRIVATE, ICU, EMERGENCY
- floor, wing (VARCHAR)
- capacity, current_occupancy (INTEGER)
- available, active (BOOLEAN)
- amenities (TEXT/JSON)
- daily_rate (DECIMAL)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: room_number, room_type, floor, wing, available, active
```

**6.2 `room_bookings`**
```sql
- id (UUID, PK)
- room_id, patient_id (UUID)
- admission_date, expected_discharge_date, actual_discharge_date (DATE)
- status (VARCHAR) -- PENDING, CONFIRMED, CANCELLED, DISCHARGED
- reason, notes (TEXT)
- created_at, updated_at
- metadata (TEXT/JSON)

Indexes: room_id, patient_id, admission_date, status, created_at
```

### 7. audit-service Database: `audit_service`

**Table: `audit_logs`**
```sql
- id (UUID, PK)
- user_id (UUID), username (VARCHAR)
- service_name (VARCHAR)
- category (VARCHAR) -- PATIENT, DOCTOR, APPOINTMENT, AUTH, etc.
- action (VARCHAR) -- CREATE, READ, UPDATE, DELETE, LOGIN, etc.
- entity_type, entity_id (VARCHAR)
- description (TEXT)
- old_value, new_value (TEXT)
- ip_address, user_agent (VARCHAR/TEXT)
- request_id, correlation_id (VARCHAR)
- success (BOOLEAN)
- error_message (TEXT)
- timestamp (TIMESTAMP)

Indexes: 14 indexes including:
- Single: user_id, service_name, category, action, entity_type, entity_id, timestamp, success, request_id, correlation_id
- Composite: user_timestamp, entity_timestamp, service_timestamp, category_action
```

## Running Migrations

### Method 1: Automatic on Application Startup

When you start each service, Flyway will automatically:
1. Check if the database exists
2. Create `flyway_schema_history` table
3. Run V1__Initial_schema.sql migration
4. Mark the migration as completed

### Method 2: Manual Migration

Run migrations manually using Maven:

```bash
# Patient Service
cd patient-service
mvn flyway:migrate

# Doctor Service
cd doctor-service
mvn flyway:migrate

# ... repeat for other services
```

### Method 3: All Services at Once

```bash
#!/bin/bash
for service in auth-service patient-service doctor-service appointment-service medical-records-service facility-service audit-service; do
  echo "Migrating $service..."
  cd /home/sanath/Projects/Clinic_mgmt/$service
  mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/${service//-/_} \
    -Dflyway.user=postgres \
    -Dflyway.password=postgres
done
```

## Database Setup

Before running migrations, create the databases:

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create all databases
CREATE DATABASE auth_service;
CREATE DATABASE patient_service;
CREATE DATABASE doctor_service;
CREATE DATABASE appointment_service;
CREATE DATABASE medical_records_service;
CREATE DATABASE facility_service;
CREATE DATABASE audit_service;

-- Verify databases
\l
```

## Flyway Commands

### Check Migration Status
```bash
mvn flyway:info
```

### Validate Migrations
```bash
mvn flyway:validate
```

### Clean Database (DANGEROUS - removes all objects)
```bash
mvn flyway:clean  # Disabled by default (clean-disabled: true)
```

### Repair (fix failed migrations)
```bash
mvn flyway:repair
```

## Migration History

After running migrations, Flyway tracks them in `flyway_schema_history` table:

```sql
SELECT * FROM flyway_schema_history;
```

| installed_rank | version | description | type | script | checksum | installed_by | installed_on | execution_time | success |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | Initial schema | SQL | V1__Initial_schema.sql | ... | postgres | 2026-01-04 ... | 125 | true |

## Adding New Migrations

To add new migrations, create files with incrementing version numbers:

```
src/main/resources/db/migration/
├── V1__Initial_schema.sql           ✓ Created
├── V2__Add_indexes.sql               (future)
├── V3__Add_patient_allergies.sql     (future)
└── V4__Modify_doctor_metadata.sql    (future)
```

**Version naming convention:**
- V{version}__{description}.sql
- Version must be unique and sequential
- Use double underscore `__` between version and description
- Description uses underscores for spaces

**Example:**
```sql
-- V2__Add_email_verification.sql
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT false;
CREATE INDEX idx_users_email_verified ON users(email_verified);
```

## Troubleshooting

### Migration Failed
```
ERROR: Migration checksum mismatch
```
**Solution:** Don't modify applied migrations. Create a new migration instead, or use `flyway:repair`.

### Database Connection Failed
```
ERROR: Connection to localhost:5432 refused
```
**Solution:** Ensure PostgreSQL is running and databases are created.

### Baseline Not Set
```
ERROR: Found non-empty schema without schema history table
```
**Solution:** Already configured with `baseline-on-migrate: true`.

## Best Practices

✓ **Never modify applied migrations** - Always create new ones
✓ **Test migrations on dev first** - Before applying to production
✓ **Use transactions** - Migrations are transactional by default
✓ **Keep migrations small** - One logical change per migration
✓ **Version control** - All migrations should be in Git
✓ **Document changes** - Use clear descriptions and comments
✓ **Backup before migrating** - Especially in production

## Verification Checklist

After starting services, verify:

- [ ] All 7 databases created
- [ ] Flyway ran successfully (check logs)
- [ ] `flyway_schema_history` table exists in each database
- [ ] All 12 tables created
- [ ] Indexes created (70+ total)
- [ ] Can query tables without errors
- [ ] R2DBC connections working

## Next Steps

1. ✅ Create databases in PostgreSQL
2. ✅ Start each service (Flyway runs automatically)
3. ⏳ Verify migrations completed successfully
4. ⏳ Test database connectivity
5. ⏳ Proceed with service layer updates
