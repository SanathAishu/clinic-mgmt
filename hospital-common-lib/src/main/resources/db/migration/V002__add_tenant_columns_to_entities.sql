-- =====================================================
-- V002: Add Tenant Columns to All Entity Tables
-- =====================================================
-- Description: Adds tenant_id column to all existing tables for multi-tenancy support
-- Author: Claude Code
-- Date: 2026-01-03

-- =====================================================
-- IMPORTANT: TEMPLATE FOR EACH SERVICE
-- =====================================================
-- Each microservice must run this migration for its own tables.
-- This file shows the pattern - adapt for your specific tables.

-- =====================================================
-- AUTH SERVICE TABLES
-- =====================================================

-- Add tenant_id to users table (if not already present)
ALTER TABLE users
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

-- For existing data, set a default tenant (update this appropriately)
UPDATE users
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

-- Make tenant_id NOT NULL after populating existing data
ALTER TABLE users
ALTER COLUMN tenant_id SET NOT NULL;

-- Add index for tenant-based queries
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenant_email ON users(tenant_id, email);

-- Update unique constraint to include tenant_id
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_email;
ALTER TABLE users ADD CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email);

-- =====================================================
-- PATIENT SERVICE TABLES
-- =====================================================

ALTER TABLE patients
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE patients
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE patients
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_patients_tenant ON patients(tenant_id);
CREATE INDEX IF NOT EXISTS idx_patients_tenant_email ON patients(tenant_id, email);

-- Update unique constraints
ALTER TABLE patients DROP CONSTRAINT IF EXISTS uk_patients_email;
ALTER TABLE patients ADD CONSTRAINT uk_patients_tenant_email UNIQUE (tenant_id, email);

-- =====================================================
-- DOCTOR SERVICE TABLES
-- =====================================================

ALTER TABLE doctors
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE doctors
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE doctors
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_doctors_tenant ON doctors(tenant_id);
CREATE INDEX IF NOT EXISTS idx_doctors_tenant_email ON doctors(tenant_id, email);
CREATE INDEX IF NOT EXISTS idx_doctors_tenant_specialty ON doctors(tenant_id, specialty);

ALTER TABLE doctors DROP CONSTRAINT IF EXISTS uk_doctors_email;
ALTER TABLE doctors ADD CONSTRAINT uk_doctors_tenant_email UNIQUE (tenant_id, email);

ALTER TABLE doctors DROP CONSTRAINT IF EXISTS uk_doctors_license;
ALTER TABLE doctors ADD CONSTRAINT uk_doctors_tenant_license UNIQUE (tenant_id, license_number);

-- =====================================================
-- APPOINTMENT SERVICE TABLES
-- =====================================================

ALTER TABLE appointments
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE appointments
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE appointments
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_appointments_tenant ON appointments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_patient ON appointments(tenant_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_doctor ON appointments(tenant_id, doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_status ON appointments(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_datetime ON appointments(tenant_id, appointment_date_time);

-- Patient snapshots (denormalized data)
ALTER TABLE patient_snapshots
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE patient_snapshots
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE patient_snapshots
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_patient_snapshots_tenant ON patient_snapshots(tenant_id);

-- Doctor snapshots (denormalized data)
ALTER TABLE doctor_snapshots
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE doctor_snapshots
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE doctor_snapshots
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_doctor_snapshots_tenant ON doctor_snapshots(tenant_id);

-- =====================================================
-- MEDICAL RECORDS SERVICE TABLES
-- =====================================================

ALTER TABLE medical_records
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE medical_records
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE medical_records
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_medical_records_tenant ON medical_records(tenant_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_tenant_patient ON medical_records(tenant_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_tenant_doctor ON medical_records(tenant_id, doctor_id);

-- Prescriptions table
ALTER TABLE prescriptions
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE prescriptions
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE prescriptions
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_prescriptions_tenant ON prescriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_tenant_patient ON prescriptions(tenant_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_tenant_doctor ON prescriptions(tenant_id, doctor_id);

-- Medical reports table
ALTER TABLE medical_reports
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE medical_reports
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE medical_reports
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_medical_reports_tenant ON medical_reports(tenant_id);
CREATE INDEX IF NOT EXISTS idx_medical_reports_tenant_patient ON medical_reports(tenant_id, patient_id);

-- =====================================================
-- FACILITY SERVICE TABLES
-- =====================================================

ALTER TABLE rooms
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE rooms
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE rooms
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_rooms_tenant ON rooms(tenant_id);
CREATE INDEX IF NOT EXISTS idx_rooms_tenant_type ON rooms(tenant_id, room_type);
CREATE INDEX IF NOT EXISTS idx_rooms_tenant_floor ON rooms(tenant_id, floor_number);

ALTER TABLE rooms DROP CONSTRAINT IF EXISTS uk_rooms_number;
ALTER TABLE rooms ADD CONSTRAINT uk_rooms_tenant_number UNIQUE (tenant_id, room_number);

-- Room bookings
ALTER TABLE room_bookings
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE room_bookings
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE room_bookings
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_room_bookings_tenant ON room_bookings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_room_bookings_tenant_room ON room_bookings(tenant_id, room_id);
CREATE INDEX IF NOT EXISTS idx_room_bookings_tenant_patient ON room_bookings(tenant_id, patient_id);

-- =====================================================
-- AUDIT SERVICE TABLES
-- =====================================================

ALTER TABLE audit_logs
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);

UPDATE audit_logs
SET tenant_id = 'default-tenant'
WHERE tenant_id IS NULL;

ALTER TABLE audit_logs
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_user ON audit_logs(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_entity ON audit_logs(tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_timestamp ON audit_logs(tenant_id, timestamp);

-- =====================================================
-- NOTIFICATION SERVICE TABLES
-- =====================================================
-- (Notification service may not need tenant_id if notifications are ephemeral)
-- Include if notifications need to be tenant-isolated

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON COLUMN users.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN patients.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN doctors.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN appointments.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN medical_records.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN prescriptions.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN medical_reports.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN rooms.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN room_bookings.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN audit_logs.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
