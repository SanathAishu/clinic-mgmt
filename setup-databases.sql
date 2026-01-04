-- PostgreSQL Database Setup Script
-- Run this script to create all databases for the Hospital Management System
--
-- Usage:
--   psql -U postgres -f setup-databases.sql
--
-- Or interactively:
--   psql -U postgres
--   \i setup-databases.sql

-- ============================================================================
-- Create Databases
-- ============================================================================

-- Drop existing databases (UNCOMMENT ONLY IF YOU WANT TO START FRESH)
-- DROP DATABASE IF EXISTS auth_service;
-- DROP DATABASE IF EXISTS patient_service;
-- DROP DATABASE IF EXISTS doctor_service;
-- DROP DATABASE IF EXISTS appointment_service;
-- DROP DATABASE IF EXISTS medical_records_service;
-- DROP DATABASE IF EXISTS facility_service;
-- DROP DATABASE IF EXISTS audit_service;

-- Create databases
CREATE DATABASE auth_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

CREATE DATABASE patient_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

CREATE DATABASE doctor_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

CREATE DATABASE appointment_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

CREATE DATABASE medical_records_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

CREATE DATABASE facility_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

CREATE DATABASE audit_service
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- ============================================================================
-- Add Database Comments
-- ============================================================================

COMMENT ON DATABASE auth_service IS 'Authentication and user management service';
COMMENT ON DATABASE patient_service IS 'Patient demographic and medical information';
COMMENT ON DATABASE doctor_service IS 'Doctor professional information and availability';
COMMENT ON DATABASE appointment_service IS 'Appointment scheduling and snapshots';
COMMENT ON DATABASE medical_records_service IS 'Medical records, prescriptions, and reports';
COMMENT ON DATABASE facility_service IS 'Hospital room management and bookings';
COMMENT ON DATABASE audit_service IS 'System-wide audit trail and logging';

-- ============================================================================
-- Optional: Create Application User (Recommended for Production)
-- ============================================================================

-- Uncomment to create a dedicated application user instead of using 'postgres'
-- CREATE USER hospital_app WITH PASSWORD 'your_secure_password_here';

-- Grant all privileges to the application user
-- GRANT ALL PRIVILEGES ON DATABASE auth_service TO hospital_app;
-- GRANT ALL PRIVILEGES ON DATABASE patient_service TO hospital_app;
-- GRANT ALL PRIVILEGES ON DATABASE doctor_service TO hospital_app;
-- GRANT ALL PRIVILEGES ON DATABASE appointment_service TO hospital_app;
-- GRANT ALL PRIVILEGES ON DATABASE medical_records_service TO hospital_app;
-- GRANT ALL PRIVILEGES ON DATABASE facility_service TO hospital_app;
-- GRANT ALL PRIVILEGES ON DATABASE audit_service TO hospital_app;

-- ============================================================================
-- Verify Databases Created
-- ============================================================================

-- List all databases
\l

-- Success message
\echo ''
\echo '============================================'
\echo '✓ All databases created successfully!'
\echo '============================================'
\echo ''
\echo 'Databases created:'
\echo '  • auth_service'
\echo '  • patient_service'
\echo '  • doctor_service'
\echo '  • appointment_service'
\echo '  • medical_records_service'
\echo '  • facility_service'
\echo '  • audit_service'
\echo ''
\echo 'Next steps:'
\echo '  1. Update application.yml with correct credentials'
\echo '  2. Start each service (Flyway will run migrations automatically)'
\echo '  3. Verify migrations: SELECT * FROM flyway_schema_history;'
\echo ''
