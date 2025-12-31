-- PostgreSQL Initialization Script for Hospital Management System
-- This script runs automatically when PostgreSQL container starts for the first time

-- Enable UUID extensions globally
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create all microservice schemas
CREATE SCHEMA IF NOT EXISTS auth_service;
CREATE SCHEMA IF NOT EXISTS patient_service;
CREATE SCHEMA IF NOT EXISTS doctor_service;
CREATE SCHEMA IF NOT EXISTS appointment_service;
CREATE SCHEMA IF NOT EXISTS medical_records_service;
CREATE SCHEMA IF NOT EXISTS facility_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS audit_service;

-- Grant all privileges on schemas to hospital_user
GRANT ALL PRIVILEGES ON SCHEMA auth_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA patient_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA doctor_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA appointment_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA medical_records_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA facility_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA notification_service TO hospital_user;
GRANT ALL PRIVILEGES ON SCHEMA audit_service TO hospital_user;

-- Grant default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA auth_service GRANT ALL ON TABLES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA patient_service GRANT ALL ON TABLES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA doctor_service GRANT ALL ON TABLES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA appointment_service GRANT ALL ON TABLES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA medical_records_service GRANT ALL ON TABLES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA facility_service GRANT ALL ON TABLES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification_service GRANT ALL ON TABLES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_service GRANT ALL ON TABLES TO hospital_user;

-- Grant sequence privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA auth_service GRANT ALL ON SEQUENCES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA patient_service GRANT ALL ON SEQUENCES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA doctor_service GRANT ALL ON SEQUENCES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA appointment_service GRANT ALL ON SEQUENCES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA medical_records_service GRANT ALL ON SEQUENCES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA facility_service GRANT ALL ON SEQUENCES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification_service GRANT ALL ON SEQUENCES TO hospital_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_service GRANT ALL ON SEQUENCES TO hospital_user;

-- Create updated_at trigger function (shared across all schemas)
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create audit trigger function for automatic audit logging
CREATE OR REPLACE FUNCTION public.audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    audit_data JSONB;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        audit_data := row_to_json(OLD)::JSONB;
    ELSE
        audit_data := row_to_json(NEW)::JSONB;
    END IF;

    -- This will be used later when audit service is ready
    -- For now, it's just a placeholder
    RETURN COALESCE(NEW, OLD);
END;
$$ language 'plpgsql';

-- Log initialization completion
DO $$
BEGIN
    RAISE NOTICE '✅ Hospital Management System schemas initialized successfully';
    RAISE NOTICE '✅ UUID extensions enabled: uuid-ossp, pgcrypto';
    RAISE NOTICE '✅ 8 schemas created: auth_service, patient_service, doctor_service, appointment_service, medical_records_service, facility_service, notification_service, audit_service';
    RAISE NOTICE '✅ Permissions granted to hospital_user';
    RAISE NOTICE '✅ Trigger functions created: update_updated_at_column, audit_trigger_function';
END $$;
