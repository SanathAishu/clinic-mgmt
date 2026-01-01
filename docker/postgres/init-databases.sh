#!/bin/bash
set -e

# Create all databases for microservices
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE auth_service;
    CREATE DATABASE patient_service;
    CREATE DATABASE doctor_service;
    CREATE DATABASE appointment_service;
    CREATE DATABASE medical_records_service;
    CREATE DATABASE facility_service;
    CREATE DATABASE notification_service;
    CREATE DATABASE audit_service;

    -- Grant privileges
    GRANT ALL PRIVILEGES ON DATABASE auth_service TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE patient_service TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE doctor_service TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE appointment_service TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE medical_records_service TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE facility_service TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE notification_service TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE audit_service TO $POSTGRES_USER;
EOSQL

echo "All databases created successfully!"
