# Database Setup Guide

## PostgreSQL Installation

PostgreSQL is not currently installed on your system. Here's how to install and set it up.

### Install PostgreSQL on Ubuntu/Debian

```bash
# Update package list
sudo apt update

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Verify installation
psql --version

# Check PostgreSQL service status
sudo systemctl status postgresql

# Start PostgreSQL if not running
sudo systemctl start postgresql

# Enable PostgreSQL to start on boot
sudo systemctl enable postgresql
```

### Install PostgreSQL on macOS

```bash
# Using Homebrew
brew install postgresql@16

# Start PostgreSQL
brew services start postgresql@16

# Verify installation
psql --version
```

### Install PostgreSQL on Other Linux Distributions

**RHEL/CentOS/Fedora:**
```bash
sudo dnf install postgresql-server postgresql-contrib
sudo postgresql-setup --initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**Arch Linux:**
```bash
sudo pacman -S postgresql
sudo -u postgres initdb -D /var/lib/postgres/data
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

## Initial PostgreSQL Configuration

### 1. Set PostgreSQL Password

After installation, set a password for the postgres user:

```bash
# Switch to postgres user
sudo -i -u postgres

# Access PostgreSQL prompt
psql

# Set password
ALTER USER postgres WITH PASSWORD 'postgres';

# Exit
\q
exit
```

### 2. Configure Authentication (Optional)

Edit PostgreSQL authentication configuration:

```bash
# Edit pg_hba.conf
sudo nano /etc/postgresql/16/main/pg_hba.conf
```

Change authentication method from `peer` to `md5` for local connections:

```
# Find this line:
local   all             postgres                                peer

# Change to:
local   all             postgres                                md5
```

Restart PostgreSQL:
```bash
sudo systemctl restart postgresql
```

## Create Databases

### Method 1: Using the Provided SQL Script (Recommended)

```bash
# Navigate to project directory
cd /home/sanath/Projects/Clinic_mgmt

# Run the setup script
psql -U postgres -f setup-databases.sql

# Enter password when prompted (default: postgres)
```

### Method 2: Manual Creation

```bash
# Connect to PostgreSQL
psql -U postgres

# Or if peer authentication is enabled:
sudo -u postgres psql
```

Then run these commands:

```sql
CREATE DATABASE auth_service;
CREATE DATABASE patient_service;
CREATE DATABASE doctor_service;
CREATE DATABASE appointment_service;
CREATE DATABASE medical_records_service;
CREATE DATABASE facility_service;
CREATE DATABASE audit_service;

-- Verify databases created
\l

-- Exit
\q
```

### Method 3: One-Line Commands

```bash
# Create all databases with one command each
psql -U postgres -c "CREATE DATABASE auth_service;"
psql -U postgres -c "CREATE DATABASE patient_service;"
psql -U postgres -c "CREATE DATABASE doctor_service;"
psql -U postgres -c "CREATE DATABASE appointment_service;"
psql -U postgres -c "CREATE DATABASE medical_records_service;"
psql -U postgres -c "CREATE DATABASE facility_service;"
psql -U postgres -c "CREATE DATABASE audit_service;"

# List all databases
psql -U postgres -c "\l"
```

## Verify Database Creation

```bash
# Connect to PostgreSQL
psql -U postgres

# List all databases
\l

# Should show:
#   auth_service
#   patient_service
#   doctor_service
#   appointment_service
#   medical_records_service
#   facility_service
#   audit_service
```

## Docker Alternative (Optional)

If you prefer using Docker:

```bash
# Run PostgreSQL in Docker
docker run -d \
  --name hospital-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_USER=postgres \
  -p 5432:5432 \
  -v hospital-data:/var/lib/postgresql/data \
  postgres:16

# Wait for PostgreSQL to start (5-10 seconds)
sleep 10

# Create databases using Docker
docker exec -i hospital-postgres psql -U postgres -f - < setup-databases.sql
```

Or use Docker Compose:

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:16
    container_name: hospital-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./setup-databases.sql:/docker-entrypoint-initdb.d/setup-databases.sql

volumes:
  postgres-data:
```

```bash
# Start PostgreSQL
docker-compose up -d

# Check logs
docker-compose logs -f postgres
```

## Test Database Connection

### Test with psql

```bash
# Test connection to each database
psql -U postgres -d auth_service -c "SELECT version();"
psql -U postgres -d patient_service -c "SELECT version();"
psql -U postgres -d doctor_service -c "SELECT version();"
# ... etc
```

### Test with Java/Spring Boot

Run one of the services:

```bash
cd patient-service
mvn spring-boot:run
```

Look for Flyway migration logs:
```
Flyway Community Edition 10.x.x
Database: jdbc:postgresql://localhost:5432/patient_service (PostgreSQL 16.x)
Successfully validated 1 migration (execution time 00:00.023s)
Creating Schema History table "public"."flyway_schema_history" ...
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - Initial schema"
Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.134s)
```

## Verify Migrations Ran

After starting a service, check the migration history:

```bash
# Connect to database
psql -U postgres -d patient_service

# Check Flyway migration history
SELECT * FROM flyway_schema_history;

# Expected output:
#  installed_rank | version | description    | type | script                  | checksum  | installed_by | installed_on        | execution_time | success
# ----------------+---------+----------------+------+-------------------------+-----------+--------------+---------------------+----------------+---------
#              1 |       1 | Initial schema | SQL  | V1__Initial_schema.sql  | 123456789 | postgres     | 2026-01-04 12:00:00 |            125 | t

# Check tables created
\dt

# Expected tables:
#  public | patients | table | postgres

# Check table structure
\d patients

# Exit
\q
```

## Troubleshooting

### Connection Refused

```
psql: error: connection to server on socket "/var/run/postgresql/.s.PGSQL.5432" failed: No such file or directory
```

**Solution:**
```bash
# Check if PostgreSQL is running
sudo systemctl status postgresql

# Start if not running
sudo systemctl start postgresql
```

### Authentication Failed

```
psql: error: FATAL: Ident authentication failed for user "postgres"
```

**Solution:** Change authentication method in `pg_hba.conf` from `peer` to `md5` (see Configuration section above).

### Database Already Exists

```
ERROR: database "patient_service" already exists
```

**Solution:** This is fine if you're re-running the script. To start fresh:

```sql
DROP DATABASE patient_service;
CREATE DATABASE patient_service;
```

### Port Already in Use

```
ERROR: could not bind IPv4 address "127.0.0.1": Address already in use
```

**Solution:** Another PostgreSQL instance is running on port 5432.

```bash
# Check what's using port 5432
sudo lsof -i :5432

# Or
sudo netstat -tlnp | grep 5432

# Stop the conflicting service or change PostgreSQL port
```

## Security Best Practices (Production)

### 1. Create Dedicated Database User

Instead of using the `postgres` superuser, create a dedicated application user:

```sql
-- Create application user
CREATE USER hospital_app WITH PASSWORD 'strong_password_here';

-- Grant privileges to specific databases
GRANT ALL PRIVILEGES ON DATABASE patient_service TO hospital_app;
GRANT ALL PRIVILEGES ON DATABASE doctor_service TO hospital_app;
-- ... repeat for all databases

-- Connect to each database and grant schema privileges
\c patient_service
GRANT ALL ON SCHEMA public TO hospital_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO hospital_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO hospital_app;
```

Update application.yml:
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/patient_service
    username: hospital_app
    password: strong_password_here
```

### 2. Use Environment Variables

```yaml
spring:
  r2dbc:
    url: ${DATABASE_URL:r2dbc:postgresql://localhost:5432/patient_service}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
```

```bash
export DATABASE_URL="r2dbc:postgresql://localhost:5432/patient_service"
export DATABASE_USERNAME="hospital_app"
export DATABASE_PASSWORD="strong_password_here"
```

### 3. Enable SSL Connections

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/patient_service?sslMode=require
```

### 4. Regular Backups

```bash
# Backup all databases
pg_dumpall -U postgres > hospital_backup_$(date +%Y%m%d).sql

# Backup specific database
pg_dump -U postgres patient_service > patient_service_backup.sql

# Restore from backup
psql -U postgres < hospital_backup_20260104.sql
```

## Next Steps

After databases are created:

1. ✅ Databases created
2. ⏳ Start services (Flyway runs migrations automatically)
3. ⏳ Verify migrations completed
4. ⏳ Test API endpoints
5. ⏳ Create REST endpoints for inter-service communication

## Quick Reference Commands

```bash
# Start PostgreSQL
sudo systemctl start postgresql

# Stop PostgreSQL
sudo systemctl stop postgresql

# Check status
sudo systemctl status postgresql

# Connect to database
psql -U postgres -d patient_service

# List databases
psql -U postgres -c "\l"

# List tables in database
psql -U postgres -d patient_service -c "\dt"

# Drop and recreate database
psql -U postgres -c "DROP DATABASE patient_service;"
psql -U postgres -c "CREATE DATABASE patient_service;"

# View PostgreSQL logs
sudo journalctl -u postgresql -f
```
