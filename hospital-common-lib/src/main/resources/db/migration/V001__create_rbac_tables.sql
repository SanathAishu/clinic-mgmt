-- =====================================================
-- V001: Create RBAC Tables for Dynamic Authorization
-- =====================================================
-- Description: Creates role-based access control tables with multi-tenancy support
-- Author: Claude Code
-- Date: 2026-01-03

-- =====================================================
-- 1. ROLES TABLE
-- =====================================================
-- Stores roles within each tenant. Roles are tenant-specific.
-- Examples: DOCTOR, NURSE, ADMIN, PATIENT, DEPARTMENT_HEAD

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,

    CONSTRAINT uk_roles_tenant_name UNIQUE (tenant_id, name)
);

-- Indexes for performance
CREATE INDEX idx_roles_tenant ON roles(tenant_id);
CREATE INDEX idx_roles_active ON roles(active);
CREATE INDEX idx_roles_tenant_active ON roles(tenant_id, active);

-- Comments for documentation
COMMENT ON TABLE roles IS 'Tenant-specific roles for RBAC system';
COMMENT ON COLUMN roles.tenant_id IS 'Tenant identifier for isolation';
COMMENT ON COLUMN roles.is_system_role IS 'System roles cannot be deleted';
COMMENT ON COLUMN roles.active IS 'Inactive roles cannot be assigned';

-- =====================================================
-- 2. PERMISSIONS TABLE
-- =====================================================
-- Stores permissions globally (not tenant-specific).
-- Format: resource:action (e.g., patient:read, medical_record:write)

CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_system_permission BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_permissions_name UNIQUE (name),
    CONSTRAINT uk_permissions_resource_action UNIQUE (resource, action)
);

-- Indexes for performance
CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_action ON permissions(action);
CREATE INDEX idx_permissions_resource_action ON permissions(resource, action);

-- Comments
COMMENT ON TABLE permissions IS 'Global permissions in resource:action format';
COMMENT ON COLUMN permissions.name IS 'Permission identifier (e.g., patient:read)';
COMMENT ON COLUMN permissions.is_system_permission IS 'System permissions cannot be deleted';

-- =====================================================
-- 3. ROLE_PERMISSIONS TABLE (Many-to-Many)
-- =====================================================
-- Maps permissions to roles. A role can have multiple permissions.

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES permissions(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- Comments
COMMENT ON TABLE role_permissions IS 'Many-to-many mapping between roles and permissions';

-- =====================================================
-- 4. USER_ROLES TABLE
-- =====================================================
-- Maps users to roles within a tenant. A user can have multiple roles.

CREATE TABLE IF NOT EXISTS user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    department VARCHAR(50),
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID,

    CONSTRAINT uk_user_roles UNIQUE (user_id, role_id, tenant_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_tenant ON user_roles(tenant_id);
CREATE INDEX idx_user_roles_user_tenant ON user_roles(user_id, tenant_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
CREATE INDEX idx_user_roles_active ON user_roles(active);
CREATE INDEX idx_user_roles_validity ON user_roles(valid_from, valid_until);
CREATE INDEX idx_user_roles_department ON user_roles(tenant_id, department);

-- Comments
COMMENT ON TABLE user_roles IS 'User-to-role assignments within tenants';
COMMENT ON COLUMN user_roles.department IS 'Optional department assignment (e.g., CARDIOLOGY)';
COMMENT ON COLUMN user_roles.valid_from IS 'Start date for time-limited role assignments';
COMMENT ON COLUMN user_roles.valid_until IS 'End date for time-limited role assignments';

-- =====================================================
-- 5. USER_RESOURCE_PERMISSIONS TABLE
-- =====================================================
-- Fine-grained permissions for specific resources.
-- Enables resource-level access control and break-glass access.

CREATE TABLE IF NOT EXISTS user_resource_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    permission VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    is_break_glass BOOLEAN DEFAULT FALSE,
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID,
    revoked_at TIMESTAMP,
    revoked_by UUID,

    CONSTRAINT uk_user_resource_permissions UNIQUE (user_id, resource_type, resource_id, permission, tenant_id)
);

-- Indexes for performance
CREATE INDEX idx_urp_user ON user_resource_permissions(user_id);
CREATE INDEX idx_urp_tenant ON user_resource_permissions(tenant_id);
CREATE INDEX idx_urp_user_tenant ON user_resource_permissions(user_id, tenant_id);
CREATE INDEX idx_urp_resource ON user_resource_permissions(resource_type, resource_id);
CREATE INDEX idx_urp_active ON user_resource_permissions(active);
CREATE INDEX idx_urp_validity ON user_resource_permissions(valid_from, valid_until);
CREATE INDEX idx_urp_break_glass ON user_resource_permissions(is_break_glass, tenant_id) WHERE is_break_glass = TRUE;
CREATE INDEX idx_urp_revoked ON user_resource_permissions(revoked_at) WHERE revoked_at IS NOT NULL;

-- Comments
COMMENT ON TABLE user_resource_permissions IS 'Fine-grained resource-level permissions';
COMMENT ON COLUMN user_resource_permissions.resource_type IS 'Type of resource (patient, medical_record, etc.)';
COMMENT ON COLUMN user_resource_permissions.resource_id IS 'Specific resource UUID';
COMMENT ON COLUMN user_resource_permissions.permission IS 'Action allowed (read, write, delete, etc.)';
COMMENT ON COLUMN user_resource_permissions.is_break_glass IS 'Emergency access flag for auditing';
COMMENT ON COLUMN user_resource_permissions.reason IS 'Reason for granting permission';

-- =====================================================
-- 6. UPDATE TRIGGERS FOR TIMESTAMP MANAGEMENT
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to roles table
CREATE TRIGGER trigger_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to permissions table
CREATE TRIGGER trigger_permissions_updated_at
    BEFORE UPDATE ON permissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 7. CLEANUP FUNCTION FOR EXPIRED PERMISSIONS
-- =====================================================

CREATE OR REPLACE FUNCTION cleanup_expired_permissions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE user_resource_permissions
    SET active = FALSE
    WHERE valid_until < CURRENT_TIMESTAMP
      AND active = TRUE;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_permissions() IS 'Deactivates expired resource permissions';
