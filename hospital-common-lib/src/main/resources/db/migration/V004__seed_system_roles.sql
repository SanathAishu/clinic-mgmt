-- =====================================================
-- V004: Seed System Roles for Each Tenant
-- =====================================================
-- Description: Creates base system roles with permissions for each tenant
-- Author: Claude Code
-- Date: 2026-01-03
--
-- NOTE: This script creates roles for the 'default-tenant'.
-- When adding new tenants, execute similar role creation for each tenant.

-- =====================================================
-- HELPER FUNCTION: Create Role with Permissions
-- =====================================================

CREATE OR REPLACE FUNCTION create_role_with_permissions(
    p_tenant_id VARCHAR,
    p_role_name VARCHAR,
    p_description VARCHAR,
    p_permission_names TEXT[]
)
RETURNS UUID AS $$
DECLARE
    v_role_id UUID;
    v_permission_id UUID;
    v_permission_name TEXT;
BEGIN
    -- Create role
    INSERT INTO roles (tenant_id, name, description, is_system_role, active)
    VALUES (p_tenant_id, p_role_name, p_description, TRUE, TRUE)
    RETURNING id INTO v_role_id;

    -- Assign permissions
    FOREACH v_permission_name IN ARRAY p_permission_names
    LOOP
        SELECT id INTO v_permission_id
        FROM permissions
        WHERE name = v_permission_name;

        IF v_permission_id IS NOT NULL THEN
            INSERT INTO role_permissions (role_id, permission_id)
            VALUES (v_role_id, v_permission_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END LOOP;

    RETURN v_role_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- CREATE ADMIN ROLE (Full System Access)
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'ADMIN',
    'System administrator with full access',
    ARRAY[
        'patient:manage',
        'doctor:manage',
        'appointment:manage',
        'medical_record:manage',
        'medical_record:break_glass',
        'prescription:manage',
        'medical_report:manage',
        'room:manage',
        'room_booking:manage',
        'user:manage',
        'role:manage',
        'permission:manage',
        'audit:manage',
        'report:manage',
        'system:admin',
        'system:config'
    ]
);

-- =====================================================
-- CREATE DOCTOR ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'DOCTOR',
    'Medical doctor with clinical access',
    ARRAY[
        'patient:read',
        'patient:write',
        'doctor:read',
        'appointment:read',
        'appointment:create',
        'appointment:update',
        'appointment:cancel',
        'medical_record:read',
        'medical_record:write',
        'medical_record:break_glass',
        'prescription:read',
        'prescription:create',
        'prescription:update',
        'medical_report:read',
        'medical_report:create',
        'medical_report:update',
        'room_booking:read',
        'audit:read'
    ]
);

-- =====================================================
-- CREATE NURSE ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'NURSE',
    'Nurse with patient care access',
    ARRAY[
        'patient:read',
        'patient:write',
        'doctor:read',
        'appointment:read',
        'appointment:update',
        'medical_record:read',
        'medical_record:write',
        'prescription:read',
        'medical_report:read',
        'room:read',
        'room_booking:read',
        'room_booking:update'
    ]
);

-- =====================================================
-- CREATE RECEPTIONIST ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'RECEPTIONIST',
    'Front desk staff with appointment management',
    ARRAY[
        'patient:read',
        'patient:write',
        'doctor:read',
        'appointment:read',
        'appointment:create',
        'appointment:update',
        'appointment:cancel',
        'room:read',
        'room_booking:read',
        'room_booking:create'
    ]
);

-- =====================================================
-- CREATE PATIENT ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'PATIENT',
    'Patient with limited self-service access',
    ARRAY[
        'appointment:read',
        'appointment:create',
        'appointment:cancel',
        'medical_record:read',
        'prescription:read',
        'medical_report:read'
    ]
);

-- =====================================================
-- CREATE DEPARTMENT_HEAD ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'DEPARTMENT_HEAD',
    'Department head with management access',
    ARRAY[
        'patient:read',
        'patient:write',
        'doctor:read',
        'doctor:write',
        'appointment:manage',
        'medical_record:read',
        'medical_record:write',
        'prescription:read',
        'prescription:create',
        'medical_report:manage',
        'room:read',
        'room:write',
        'room_booking:manage',
        'user:read',
        'role:read',
        'audit:read',
        'report:read',
        'report:create'
    ]
);

-- =====================================================
-- CREATE LAB_TECHNICIAN ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'LAB_TECHNICIAN',
    'Laboratory technician with report access',
    ARRAY[
        'patient:read',
        'medical_report:read',
        'medical_report:create',
        'medical_report:update'
    ]
);

-- =====================================================
-- CREATE PHARMACIST ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'PHARMACIST',
    'Pharmacist with prescription access',
    ARRAY[
        'patient:read',
        'prescription:read',
        'prescription:update',
        'medical_record:read'
    ]
);

-- =====================================================
-- CREATE EMERGENCY_STAFF ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'EMERGENCY_STAFF',
    'Emergency department staff with extended access',
    ARRAY[
        'patient:read',
        'patient:write',
        'doctor:read',
        'appointment:read',
        'appointment:create',
        'medical_record:read',
        'medical_record:write',
        'medical_record:break_glass',
        'prescription:read',
        'prescription:create',
        'medical_report:read',
        'room_booking:read',
        'room_booking:create'
    ]
);

-- =====================================================
-- CREATE AUDITOR ROLE
-- =====================================================

SELECT create_role_with_permissions(
    'default-tenant',
    'AUDITOR',
    'Auditor with read-only access for compliance',
    ARRAY[
        'patient:read',
        'doctor:read',
        'appointment:read',
        'medical_record:read',
        'prescription:read',
        'medical_report:read',
        'room:read',
        'room_booking:read',
        'user:read',
        'role:read',
        'permission:read',
        'audit:read',
        'audit:manage',
        'report:read',
        'report:create'
    ]
);

-- =====================================================
-- VERIFY ROLES CREATED
-- =====================================================

DO $$
DECLARE
    role_count INTEGER;
    total_permissions INTEGER;
BEGIN
    SELECT COUNT(*) INTO role_count
    FROM roles
    WHERE tenant_id = 'default-tenant' AND is_system_role = TRUE;

    SELECT COUNT(*) INTO total_permissions
    FROM role_permissions rp
    JOIN roles r ON rp.role_id = r.id
    WHERE r.tenant_id = 'default-tenant';

    RAISE NOTICE 'System roles created for default-tenant: %', role_count;
    RAISE NOTICE 'Total role-permission mappings: %', total_permissions;
END $$;

-- =====================================================
-- CLEANUP HELPER FUNCTION
-- =====================================================
-- Optional: Drop the helper function if not needed for future tenants
-- DROP FUNCTION IF EXISTS create_role_with_permissions(VARCHAR, VARCHAR, VARCHAR, TEXT[]);
