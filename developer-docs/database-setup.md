# Database Setup Guide

This guide covers PostgreSQL setup, database creation, Flyway migrations, and R2DBC configuration.

## Quick Setup (Docker)

The easiest way to set up the database:

```bash
# Start PostgreSQL and Redis containers
docker-compose up -d

# Verify containers are running
docker-compose ps
```

The Docker setup automatically creates all required databases.

## Manual PostgreSQL Installation

### Ubuntu/Debian

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### macOS (Homebrew)

```bash
brew install postgresql@16
brew services start postgresql@16
```

### RHEL/CentOS/Fedora

```bash
sudo dnf install postgresql-server postgresql-contrib
sudo postgresql-setup --initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

## Database Creation

### Option 1: Using Docker (Recommended)

Databases are created automatically via `docker/postgres/init-databases.sh`.

### Option 2: Using SQL Script

```bash
psql -U postgres -f setup-databases.sql
```

### Option 3: Manual Creation

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create databases
CREATE DATABASE auth_service;
CREATE DATABASE patient_service;
CREATE DATABASE doctor_service;
CREATE DATABASE appointment_service;
CREATE DATABASE medical_records_service;
CREATE DATABASE facility_service;
CREATE DATABASE audit_service;

-- Verify
\l
```

## Database Schema

### Service-Database Mapping

| Service | Database | Tables |
|---------|----------|--------|
| auth-service | auth_service | users |
| patient-service | patient_service | patients |
| doctor-service | doctor_service | doctors |
| appointment-service | appointment_service | appointments, patient_snapshots, doctor_snapshots |
| medical-records-service | medical_records_service | medical_records, prescriptions, medical_reports |
| facility-service | facility_service | rooms, room_bookings |
| audit-service | audit_service | audit_logs |

### Table Schemas

#### users (auth_service)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    gender VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT true,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
);
```

#### patients (patient_service)

```sql
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
    blood_group VARCHAR(20),
    medical_history TEXT,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### doctors (doctor_service)

```sql
CREATE TABLE doctors (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    gender VARCHAR(10),
    specialty VARCHAR(50) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    years_of_experience INTEGER,
    qualifications TEXT,
    biography TEXT,
    clinic_address VARCHAR(500),
    consultation_fee DECIMAL(10, 2),
    available BOOLEAN DEFAULT true,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

## Flyway Migrations

### Overview

Flyway manages database schema versioning. Migrations run automatically on service startup.

### Migration Location

```
{service}/src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Add_columns.sql
└── ...
```

### Naming Convention

```
V{version}__{description}.sql
```

- Version must be unique and sequential
- Use double underscore between version and description
- Description uses underscores for spaces

### Configuration

Each service's `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    clean-disabled: true
```

### Manual Migration Commands

```bash
# Run migrations
cd patient-service
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Repair failed migrations
mvn flyway:repair
```

### Adding New Migrations

1. Create a new file in `src/main/resources/db/migration/`:

```sql
-- V2__Add_phone_verification.sql
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN DEFAULT false;
CREATE INDEX idx_users_phone_verified ON users(phone_verified);
```

2. Restart the service or run `mvn flyway:migrate`

### Best Practices

- Never modify applied migrations
- Test migrations in development first
- Keep migrations small and focused
- Use transactions (default behavior)
- Backup before production migrations

## R2DBC Configuration

### Connection URL Format

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/{database}
    username: postgres
    password: postgres
```

### Connection Pool Settings

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 20
      max-acquire-time: 2s
      max-create-connection-time: 2s
      validation-query: SELECT 1
```

### Environment Variables

Override defaults with environment variables:

```bash
export R2DBC_URL="r2dbc:postgresql://prod-db:5432/patient_service"
export R2DBC_USERNAME="app_user"
export R2DBC_PASSWORD="secure_password"
```

### Debug Logging

Enable SQL query logging:

```yaml
logging:
  level:
    io.r2dbc.postgresql.QUERY: DEBUG
    io.r2dbc.postgresql.PARAM: DEBUG
```

### Connection String Variants

```
# Development
r2dbc:postgresql://localhost:5432/patient_service

# With SSL
r2dbc:postgresql://host:5432/patient_service?sslMode=require

# Custom Schema
r2dbc:postgresql://localhost:5432/patient_service?currentSchema=hospital

# Multiple Hosts (Failover)
r2dbc:postgresql://host1:5432,host2:5432/patient_service
```

## Entity Configuration for R2DBC

### Persistable Interface

R2DBC entities must implement `Persistable<UUID>` for ID generation:

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

    private String name;
    private String email;
    // ... other fields

    @Override
    public boolean isNew() {
        return isNew;
    }
}
```

### Service Layer

Set ID and isNew flag before saving:

```java
public Mono<Patient> createPatient(CreatePatientRequest request) {
    Patient patient = Patient.builder()
        .id(UUID.randomUUID())
        .isNew(true)
        .name(request.getName())
        .email(request.getEmail())
        .build();
    return patientRepository.save(patient);
}
```

## Troubleshooting

### Connection Refused

```
Error: Connection refused: localhost/127.0.0.1:5432
```

**Solution:** Start PostgreSQL:
```bash
docker-compose up -d postgres
# or
sudo systemctl start postgresql
```

### Authentication Failed

```
Error: password authentication failed for user "postgres"
```

**Solution:** Verify credentials in `application.yml` or reset password:
```sql
ALTER USER postgres WITH PASSWORD 'postgres';
```

### Database Does Not Exist

```
Error: database "patient_service" does not exist
```

**Solution:** Create the database:
```sql
CREATE DATABASE patient_service;
```

### Migration Checksum Mismatch

```
Error: Migration checksum mismatch
```

**Solution:** Never modify applied migrations. Create a new migration or use:
```bash
mvn flyway:repair
```

### Too Many Connections

```
Error: FATAL: remaining connection slots are reserved
```

**Solution:** Reduce pool size or increase PostgreSQL max_connections:
```sql
ALTER SYSTEM SET max_connections = 200;
SELECT pg_reload_conf();
```

## Production Recommendations

### 1. Dedicated Database User

```sql
CREATE USER hospital_app WITH PASSWORD 'strong_password';
GRANT ALL PRIVILEGES ON DATABASE patient_service TO hospital_app;
\c patient_service
GRANT ALL ON SCHEMA public TO hospital_app;
```

### 2. Environment Variables

```yaml
spring:
  r2dbc:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

### 3. SSL Connections

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://host:5432/db?sslMode=require
```

### 4. Connection Pool Tuning

| Environment | Initial | Max |
|-------------|---------|-----|
| Development | 5 | 10 |
| Staging | 10 | 20 |
| Production | 20 | 50 |

### 5. Regular Backups

```bash
# Backup all databases
pg_dumpall -U postgres > backup_$(date +%Y%m%d).sql

# Backup specific database
pg_dump -U postgres patient_service > patient_backup.sql

# Restore
psql -U postgres < backup.sql
```

## Verification Checklist

After setup, verify:

- [ ] PostgreSQL is running
- [ ] All 7 databases exist (`\l` in psql)
- [ ] Services start without database errors
- [ ] `flyway_schema_history` table exists in each database
- [ ] All tables created (check with `\dt`)
- [ ] Indexes created
- [ ] API endpoints return data
