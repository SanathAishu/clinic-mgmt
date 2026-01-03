-- =====================================================
-- V003: Seed System Permissions
-- =====================================================
-- Description: Inserts initial system permissions for all resources
-- Author: Claude Code
-- Date: 2026-01-03

-- =====================================================
-- PATIENT RESOURCE PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('patient:read', 'patient', 'read', 'View patient information', TRUE),
    ('patient:write', 'patient', 'write', 'Create or update patient records', TRUE),
    ('patient:delete', 'patient', 'delete', 'Delete patient records', TRUE),
    ('patient:manage', 'patient', 'manage', 'Full patient management access', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- DOCTOR RESOURCE PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('doctor:read', 'doctor', 'read', 'View doctor information', TRUE),
    ('doctor:write', 'doctor', 'write', 'Create or update doctor records', TRUE),
    ('doctor:delete', 'doctor', 'delete', 'Delete doctor records', TRUE),
    ('doctor:manage', 'doctor', 'manage', 'Full doctor management access', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- APPOINTMENT RESOURCE PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('appointment:read', 'appointment', 'read', 'View appointments', TRUE),
    ('appointment:create', 'appointment', 'create', 'Create new appointments', TRUE),
    ('appointment:update', 'appointment', 'update', 'Update appointment details', TRUE),
    ('appointment:cancel', 'appointment', 'cancel', 'Cancel appointments', TRUE),
    ('appointment:delete', 'appointment', 'delete', 'Delete appointment records', TRUE),
    ('appointment:manage', 'appointment', 'manage', 'Full appointment management', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- MEDICAL RECORD RESOURCE PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('medical_record:read', 'medical_record', 'read', 'View medical records', TRUE),
    ('medical_record:write', 'medical_record', 'write', 'Create or update medical records', TRUE),
    ('medical_record:delete', 'medical_record', 'delete', 'Delete medical records', TRUE),
    ('medical_record:manage', 'medical_record', 'manage', 'Full medical record management', TRUE),
    ('medical_record:break_glass', 'medical_record', 'break_glass', 'Emergency access to medical records', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- PRESCRIPTION RESOURCE PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('prescription:read', 'prescription', 'read', 'View prescriptions', TRUE),
    ('prescription:create', 'prescription', 'create', 'Create new prescriptions', TRUE),
    ('prescription:update', 'prescription', 'update', 'Update prescription details', TRUE),
    ('prescription:delete', 'prescription', 'delete', 'Delete prescriptions', TRUE),
    ('prescription:manage', 'prescription', 'manage', 'Full prescription management', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- MEDICAL REPORT RESOURCE PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('medical_report:read', 'medical_report', 'read', 'View medical reports', TRUE),
    ('medical_report:create', 'medical_report', 'create', 'Create medical reports', TRUE),
    ('medical_report:update', 'medical_report', 'update', 'Update medical reports', TRUE),
    ('medical_report:delete', 'medical_report', 'delete', 'Delete medical reports', TRUE),
    ('medical_report:manage', 'medical_report', 'manage', 'Full medical report management', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- ROOM & FACILITY RESOURCE PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('room:read', 'room', 'read', 'View room information', TRUE),
    ('room:write', 'room', 'write', 'Create or update rooms', TRUE),
    ('room:delete', 'room', 'delete', 'Delete rooms', TRUE),
    ('room:manage', 'room', 'manage', 'Full room management', TRUE),
    ('room_booking:read', 'room_booking', 'read', 'View room bookings', TRUE),
    ('room_booking:create', 'room_booking', 'create', 'Create room bookings', TRUE),
    ('room_booking:update', 'room_booking', 'update', 'Update room bookings', TRUE),
    ('room_booking:delete', 'room_booking', 'delete', 'Delete room bookings', TRUE),
    ('room_booking:manage', 'room_booking', 'manage', 'Full booking management', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- USER MANAGEMENT PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('user:read', 'user', 'read', 'View user information', TRUE),
    ('user:create', 'user', 'create', 'Create new users', TRUE),
    ('user:update', 'user', 'update', 'Update user information', TRUE),
    ('user:delete', 'user', 'delete', 'Delete users', TRUE),
    ('user:manage', 'user', 'manage', 'Full user management', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- ROLE & PERMISSION MANAGEMENT
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('role:read', 'role', 'read', 'View roles', TRUE),
    ('role:create', 'role', 'create', 'Create new roles', TRUE),
    ('role:update', 'role', 'update', 'Update role information', TRUE),
    ('role:delete', 'role', 'delete', 'Delete roles', TRUE),
    ('role:assign', 'role', 'assign', 'Assign roles to users', TRUE),
    ('role:manage', 'role', 'manage', 'Full role management', TRUE),
    ('permission:read', 'permission', 'read', 'View permissions', TRUE),
    ('permission:manage', 'permission', 'manage', 'Manage permissions', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- AUDIT & REPORTING PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('audit:read', 'audit', 'read', 'View audit logs', TRUE),
    ('audit:manage', 'audit', 'manage', 'Manage audit logs', TRUE),
    ('report:read', 'report', 'read', 'View reports', TRUE),
    ('report:create', 'report', 'create', 'Create reports', TRUE),
    ('report:manage', 'report', 'manage', 'Full report management', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- SYSTEM ADMINISTRATION PERMISSIONS
-- =====================================================

INSERT INTO permissions (name, resource, action, description, is_system_permission) VALUES
    ('system:admin', 'system', 'admin', 'Full system administration access', TRUE),
    ('system:config', 'system', 'config', 'Manage system configuration', TRUE),
    ('tenant:create', 'tenant', 'create', 'Create new tenants', TRUE),
    ('tenant:manage', 'tenant', 'manage', 'Manage tenant settings', TRUE)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- VERIFY PERMISSIONS INSERTED
-- =====================================================

DO $$
DECLARE
    permission_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO permission_count FROM permissions WHERE is_system_permission = TRUE;
    RAISE NOTICE 'Total system permissions created: %', permission_count;
END $$;
