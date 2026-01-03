-- ===================================================================
-- V007: Create Audit Logs Table
-- ===================================================================
-- Creates comprehensive audit logging table for tracking all system
-- events, changes, and user actions.
--
-- Features:
-- - Multi-tenant support (tenant_id column)
-- - Immutable audit trail (no updates/deletes)
-- - DPDPA compliance (7-year retention minimum)
-- - Comprehensive indexing for performance
-- ===================================================================

-- Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    user_id UUID,
    user_email VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    description VARCHAR(500),
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    event_id UUID,
    http_method VARCHAR(10),
    request_path VARCHAR(500),
    status_code INTEGER,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_audit_tenant_timestamp ON audit_logs(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_user_id ON audit_logs(user_id, timestamp DESC);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id, timestamp DESC);
CREATE INDEX idx_audit_action ON audit_logs(action, timestamp DESC);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_event_id ON audit_logs(event_id);
CREATE INDEX idx_audit_status_code ON audit_logs(status_code);

-- Add comment to table
COMMENT ON TABLE audit_logs IS 'Audit log table for tracking all system events and changes (DPDPA compliance)';

-- Add comments to columns
COMMENT ON COLUMN audit_logs.tenant_id IS 'Tenant discriminator for multi-tenancy isolation';
COMMENT ON COLUMN audit_logs.user_id IS 'User who performed the action';
COMMENT ON COLUMN audit_logs.action IS 'Action performed (CREATE, UPDATE, DELETE, LOGIN, etc.)';
COMMENT ON COLUMN audit_logs.resource_type IS 'Type of resource affected (USER, PATIENT, APPOINTMENT, etc.)';
COMMENT ON COLUMN audit_logs.resource_id IS 'ID of the affected resource';
COMMENT ON COLUMN audit_logs.old_value IS 'Previous value (JSON format) before change';
COMMENT ON COLUMN audit_logs.new_value IS 'New value (JSON format) after change';
COMMENT ON COLUMN audit_logs.event_id IS 'Source event ID for correlation with domain events';
COMMENT ON COLUMN audit_logs.timestamp IS 'When the action occurred (immutable)';

-- ===================================================================
-- Row-Level Security for Audit Logs
-- ===================================================================

-- Enable RLS on audit_logs
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see audit logs for their tenant
CREATE POLICY audit_logs_tenant_isolation ON audit_logs
    FOR SELECT
    USING (tenant_id = current_setting('app.current_tenant_id', true));

-- Policy: Only allow INSERT (audit logs are immutable - no updates/deletes)
CREATE POLICY audit_logs_insert_only ON audit_logs
    FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- ===================================================================
-- Retention Policy Function (for DPDPA compliance)
-- ===================================================================
-- Audit logs must be retained for minimum 7 years for healthcare data.
-- This function can be called by a scheduled job to archive old logs.

CREATE OR REPLACE FUNCTION archive_old_audit_logs(retention_years INTEGER DEFAULT 7)
RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- In production, move to archive table instead of DELETE
    -- For now, just count how many would be archived
    SELECT COUNT(*)
    INTO archived_count
    FROM audit_logs
    WHERE timestamp < (CURRENT_TIMESTAMP - INTERVAL '1 year' * retention_years);

    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION archive_old_audit_logs IS 'Returns count of audit logs older than retention period (7 years default for DPDPA compliance)';

-- ===================================================================
-- Audit Log Statistics Function
-- ===================================================================

CREATE OR REPLACE FUNCTION get_audit_statistics(p_tenant_id VARCHAR)
RETURNS TABLE (
    total_logs BIGINT,
    creates BIGINT,
    updates BIGINT,
    deletes BIGINT,
    logins BIGINT,
    failed_operations BIGINT,
    unique_users BIGINT,
    date_range_start TIMESTAMP,
    date_range_end TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT as total_logs,
        COUNT(*) FILTER (WHERE action = 'CREATE')::BIGINT as creates,
        COUNT(*) FILTER (WHERE action = 'UPDATE')::BIGINT as updates,
        COUNT(*) FILTER (WHERE action = 'DELETE')::BIGINT as deletes,
        COUNT(*) FILTER (WHERE action = 'LOGIN')::BIGINT as logins,
        COUNT(*) FILTER (WHERE status_code >= 400)::BIGINT as failed_operations,
        COUNT(DISTINCT user_id)::BIGINT as unique_users,
        MIN(timestamp) as date_range_start,
        MAX(timestamp) as date_range_end
    FROM audit_logs
    WHERE tenant_id = p_tenant_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_audit_statistics IS 'Get comprehensive audit statistics for a tenant';

-- ===================================================================
-- Grant Permissions
-- ===================================================================

-- Grant SELECT to all authenticated users (RLS enforces tenant isolation)
GRANT SELECT ON audit_logs TO PUBLIC;

-- Grant INSERT to application (for audit logging)
GRANT INSERT ON audit_logs TO PUBLIC;

-- NO UPDATE or DELETE permissions (immutable audit trail)

-- ===================================================================
-- Verification Queries
-- ===================================================================

-- Verify table created
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'audit_logs'
    ) THEN
        RAISE NOTICE 'audit_logs table created successfully';
    ELSE
        RAISE EXCEPTION 'Failed to create audit_logs table';
    END IF;
END $$;

-- Verify indexes created
DO $$
DECLARE
    index_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO index_count
    FROM pg_indexes
    WHERE tablename = 'audit_logs';

    IF index_count >= 7 THEN
        RAISE NOTICE 'All % indexes created for audit_logs', index_count;
    ELSE
        RAISE WARNING 'Expected 7+ indexes, found %', index_count;
    END IF;
END $$;

-- ===================================================================
-- Sample Data (Development Only)
-- ===================================================================

-- Insert sample audit log for testing (only in development)
-- INSERT INTO audit_logs (tenant_id, user_id, user_email, action, resource_type, resource_id, description)
-- VALUES ('default-tenant', gen_random_uuid(), 'admin@hospital.com', 'CREATE', 'USER', gen_random_uuid(), 'Sample audit log entry');
