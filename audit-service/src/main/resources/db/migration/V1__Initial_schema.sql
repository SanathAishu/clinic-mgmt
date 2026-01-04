-- Audit Service Initial Schema
-- Creates audit_logs table for comprehensive audit trail

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    username VARCHAR(255),
    service_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    description TEXT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(100),
    correlation_id VARCHAR(100),
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance (critical for audit queries)
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_service_name ON audit_logs(service_name);
CREATE INDEX idx_audit_logs_category ON audit_logs(category);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_success ON audit_logs(success);
CREATE INDEX idx_audit_logs_request_id ON audit_logs(request_id);
CREATE INDEX idx_audit_logs_correlation_id ON audit_logs(correlation_id);

-- Composite indexes for common queries
CREATE INDEX idx_audit_logs_user_timestamp ON audit_logs(user_id, timestamp DESC);
CREATE INDEX idx_audit_logs_entity_timestamp ON audit_logs(entity_type, entity_id, timestamp DESC);
CREATE INDEX idx_audit_logs_service_timestamp ON audit_logs(service_name, timestamp DESC);
CREATE INDEX idx_audit_logs_category_action ON audit_logs(category, action);

-- Comments for documentation
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all system actions';
COMMENT ON COLUMN audit_logs.user_id IS 'UUID of user who performed the action';
COMMENT ON COLUMN audit_logs.service_name IS 'Name of microservice that logged the event';
COMMENT ON COLUMN audit_logs.category IS 'PATIENT, DOCTOR, APPOINTMENT, AUTH, etc.';
COMMENT ON COLUMN audit_logs.action IS 'CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, etc.';
COMMENT ON COLUMN audit_logs.entity_type IS 'Type of entity affected (Patient, Doctor, etc.)';
COMMENT ON COLUMN audit_logs.entity_id IS 'ID of the affected entity';
COMMENT ON COLUMN audit_logs.old_value IS 'Previous value (for updates)';
COMMENT ON COLUMN audit_logs.new_value IS 'New value (for creates/updates)';
COMMENT ON COLUMN audit_logs.ip_address IS 'Client IP address';
COMMENT ON COLUMN audit_logs.user_agent IS 'Client user agent string';
COMMENT ON COLUMN audit_logs.request_id IS 'Unique request identifier';
COMMENT ON COLUMN audit_logs.correlation_id IS 'Distributed tracing correlation ID';
COMMENT ON COLUMN audit_logs.success IS 'Whether the action succeeded';
COMMENT ON COLUMN audit_logs.error_message IS 'Error details if action failed';
