-- =====================================================
-- V005: Enable PostgreSQL Row-Level Security (RLS)
-- =====================================================
-- Description: Implements RLS policies for automatic tenant isolation
-- Author: Claude Code
-- Date: 2026-01-03
--
-- IMPORTANT: This provides defense-in-depth but should NOT be the only
-- tenant isolation mechanism. Application-level filtering is still required.

-- =====================================================
-- ENABLE RLS ON TENANT-SPECIFIC TABLES
-- =====================================================

-- AUTH SERVICE
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- PATIENT SERVICE
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;

-- DOCTOR SERVICE
ALTER TABLE doctors ENABLE ROW LEVEL SECURITY;

-- APPOINTMENT SERVICE
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctor_snapshots ENABLE ROW LEVEL SECURITY;

-- MEDICAL RECORDS SERVICE
ALTER TABLE medical_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE medical_reports ENABLE ROW LEVEL SECURITY;

-- FACILITY SERVICE
ALTER TABLE rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE room_bookings ENABLE ROW LEVEL SECURITY;

-- AUDIT SERVICE
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- RBAC TABLES (tenant-specific)
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_resource_permissions ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- CREATE RLS POLICY FUNCTION
-- =====================================================
-- This function retrieves the current tenant ID from session variable.
-- The application must SET this at the beginning of each transaction.

CREATE OR REPLACE FUNCTION current_tenant_id()
RETURNS VARCHAR AS $$
BEGIN
    -- Get tenant_id from session variable
    -- Application code must execute: SET LOCAL app.current_tenant_id = 'hosp-001';
    RETURN current_setting('app.current_tenant_id', TRUE);
EXCEPTION
    WHEN OTHERS THEN
        -- If not set, return NULL (will deny all access via RLS)
        RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION current_tenant_id() IS
    'Returns current tenant ID from session variable app.current_tenant_id';

-- =====================================================
-- CREATE RLS POLICIES FOR ALL TABLES
-- =====================================================
-- Policy pattern: Allow operations only for rows matching current tenant

-- ============ AUTH SERVICE ============

CREATE POLICY tenant_isolation_policy ON users
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ============ PATIENT SERVICE ============

CREATE POLICY tenant_isolation_policy ON patients
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ============ DOCTOR SERVICE ============

CREATE POLICY tenant_isolation_policy ON doctors
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ============ APPOINTMENT SERVICE ============

CREATE POLICY tenant_isolation_policy ON appointments
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON patient_snapshots
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON doctor_snapshots
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ============ MEDICAL RECORDS SERVICE ============

CREATE POLICY tenant_isolation_policy ON medical_records
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON prescriptions
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON medical_reports
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ============ FACILITY SERVICE ============

CREATE POLICY tenant_isolation_policy ON rooms
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON room_bookings
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ============ AUDIT SERVICE ============

CREATE POLICY tenant_isolation_policy ON audit_logs
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ============ RBAC TABLES ============

CREATE POLICY tenant_isolation_policy ON roles
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON user_roles
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON user_resource_permissions
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- =====================================================
-- BYPASS RLS FOR SUPERUSER/ADMIN ROLE
-- =====================================================
-- Create a special database role that can bypass RLS for admin operations

-- CREATE ROLE hospital_admin WITH BYPASSRLS;

-- Grant this role to specific admin users when needed:
-- GRANT hospital_admin TO admin_user;

-- =====================================================
-- HELPER FUNCTION: Set Tenant Context
-- =====================================================
-- Application code should call this at the start of each transaction

CREATE OR REPLACE FUNCTION set_tenant_context(p_tenant_id VARCHAR)
RETURNS VOID AS $$
BEGIN
    EXECUTE format('SET LOCAL app.current_tenant_id = %L', p_tenant_id);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION set_tenant_context(VARCHAR) IS
    'Sets the current tenant ID for RLS policies. Call at start of each transaction.';

-- Usage example in application code:
-- SELECT set_tenant_context('hosp-001');
-- ... perform operations ...
-- COMMIT; (or ROLLBACK)

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Check which tables have RLS enabled
DO $$
DECLARE
    rls_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO rls_count
    FROM pg_tables t
    JOIN pg_class c ON c.relname = t.tablename
    WHERE c.relrowsecurity = TRUE
      AND t.schemaname = 'public';

    RAISE NOTICE 'Tables with RLS enabled: %', rls_count;
END $$;

-- List all RLS policies
DO $$
DECLARE
    policy_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO policy_count
    FROM pg_policies
    WHERE schemaname = 'public';

    RAISE NOTICE 'Total RLS policies created: %', policy_count;
END $$;

-- =====================================================
-- IMPORTANT NOTES FOR APPLICATION DEVELOPERS
-- =====================================================
--
-- 1. At the start of each database transaction, execute:
--    SELECT set_tenant_context('tenant-id-from-jwt');
--
-- 2. This is defense-in-depth. Still use application-level WHERE clauses:
--    WHERE tenant_id = :tenantId
--
-- 3. For Hibernate Reactive / Panache, you can use @Filter:
--    @Entity
--    @FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
--    @Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
--    public class Patient { ... }
--
-- 4. RLS policies only work when session variable is set correctly.
--    If not set, all queries will return empty results (fail-secure).
--
-- 5. Test RLS policies thoroughly before production deployment.
