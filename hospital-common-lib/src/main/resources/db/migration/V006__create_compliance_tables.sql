-- =====================================================
-- V006: Create Compliance Tables for DPDPA 2023
-- =====================================================
-- Description: Creates tables for consent management and data breach logging
-- Author: Claude Code
-- Date: 2026-01-03
--
-- DPDPA Requirements:
-- - Purpose-specific consent collection and management
-- - Data breach notification and logging
-- - Audit trail for all compliance activities

-- =====================================================
-- 1. CONSENTS TABLE
-- =====================================================
-- Tracks patient consent for various data processing purposes.

CREATE TABLE IF NOT EXISTS consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    patient_id UUID NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    consent_method VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    withdrawn_at TIMESTAMP,
    withdrawal_reason VARCHAR(500),
    recorded_by UUID,
    parent_consent_id UUID,
    consent_version VARCHAR(20),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_consents_parent FOREIGN KEY (parent_consent_id)
        REFERENCES consents(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_consents_patient ON consents(patient_id);
CREATE INDEX idx_consents_tenant ON consents(tenant_id);
CREATE INDEX idx_consents_patient_tenant ON consents(patient_id, tenant_id);
CREATE INDEX idx_consents_purpose ON consents(purpose);
CREATE INDEX idx_consents_status ON consents(status);
CREATE INDEX idx_consents_expiry ON consents(expires_at);
CREATE INDEX idx_consents_granted ON consents(granted_at);
CREATE INDEX idx_consents_withdrawn ON consents(withdrawn_at) WHERE withdrawn_at IS NOT NULL;
CREATE INDEX idx_consents_active ON consents(tenant_id, patient_id, purpose, status)
    WHERE status = 'ACTIVE' AND withdrawn_at IS NULL;

-- Comments
COMMENT ON TABLE consents IS 'DPDPA 2023: Patient consent tracking for data processing purposes';
COMMENT ON COLUMN consents.purpose IS 'Purpose: TREATMENT, RESEARCH, MARKETING, DATA_SHARING, EMERGENCY, ANALYTICS, etc.';
COMMENT ON COLUMN consents.status IS 'Status: ACTIVE, WITHDRAWN, EXPIRED, PENDING, DENIED, SUPERSEDED';
COMMENT ON COLUMN consents.consent_method IS 'Method: WEB_FORM, MOBILE_APP, PAPER_FORM, VERBAL, EMAIL, SMS, PHONE, IMPLIED';
COMMENT ON COLUMN consents.ip_address IS 'IP address for audit trail';
COMMENT ON COLUMN consents.user_agent IS 'User agent for audit trail';
COMMENT ON COLUMN consents.parent_consent_id IS 'Links to original consent if this is a renewal';

-- =====================================================
-- 2. DATA_BREACH_LOGS TABLE
-- =====================================================
-- Maintains comprehensive records of all data breaches.
-- DPDPA Section 8: Data breach notification requirements

CREATE TABLE IF NOT EXISTS data_breach_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    incident_id VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_type_affected VARCHAR(100),
    individuals_affected INTEGER,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    occurred_at TIMESTAMP,
    detection_method VARCHAR(255),
    root_cause TEXT,
    dpb_notified_at TIMESTAMP,
    dpb_reference VARCHAR(100),
    individuals_notified_at TIMESTAMP,
    containment_actions TEXT,
    remediation_steps TEXT,
    resolved_at TIMESTAMP,
    reported_by UUID,
    assigned_to UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_breach_tenant ON data_breach_logs(tenant_id);
CREATE INDEX idx_breach_incident ON data_breach_logs(incident_id);
CREATE INDEX idx_breach_severity ON data_breach_logs(severity);
CREATE INDEX idx_breach_status ON data_breach_logs(status);
CREATE INDEX idx_breach_detected ON data_breach_logs(detected_at);
CREATE INDEX idx_breach_dpb_notified ON data_breach_logs(dpb_notified_at);
CREATE INDEX idx_breach_resolved ON data_breach_logs(resolved_at);
CREATE INDEX idx_breach_unresolved ON data_breach_logs(tenant_id, status)
    WHERE status NOT IN ('RESOLVED', 'CLOSED');
CREATE INDEX idx_breach_pending_notification ON data_breach_logs(tenant_id, severity)
    WHERE dpb_notified_at IS NULL AND severity IN ('CRITICAL', 'HIGH');

-- Comments
COMMENT ON TABLE data_breach_logs IS 'DPDPA Section 8: Data breach notification and tracking';
COMMENT ON COLUMN data_breach_logs.incident_id IS 'Unique incident identifier (e.g., BR-20240103-ABC123)';
COMMENT ON COLUMN data_breach_logs.severity IS 'Severity: CRITICAL, HIGH, MEDIUM, LOW';
COMMENT ON COLUMN data_breach_logs.status IS 'Status: DETECTED, UNDER_INVESTIGATION, CONTAINED, DPB_NOTIFIED, INDIVIDUALS_NOTIFIED, REMEDIATION_IN_PROGRESS, RESOLVED, CLOSED';
COMMENT ON COLUMN data_breach_logs.dpb_notified_at IS 'When Data Protection Board was notified';
COMMENT ON COLUMN data_breach_logs.dpb_reference IS 'Reference number from DPB notification';

-- =====================================================
-- 3. CONSENT AUDIT TRAIL
-- =====================================================
-- Separate table for tracking all consent-related actions

CREATE TABLE IF NOT EXISTS consent_audit_trail (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_id UUID NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    patient_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    action_by UUID,
    action_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    reason VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    additional_details JSONB,

    CONSTRAINT fk_consent_audit_consent FOREIGN KEY (consent_id)
        REFERENCES consents(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_consent_audit_consent ON consent_audit_trail(consent_id);
CREATE INDEX idx_consent_audit_patient ON consent_audit_trail(patient_id, tenant_id);
CREATE INDEX idx_consent_audit_action ON consent_audit_trail(action);
CREATE INDEX idx_consent_audit_timestamp ON consent_audit_trail(action_at);

-- Comments
COMMENT ON TABLE consent_audit_trail IS 'Audit trail for all consent-related actions';
COMMENT ON COLUMN consent_audit_trail.action IS 'Action: GRANTED, WITHDRAWN, EXPIRED, RENEWED, MODIFIED';
COMMENT ON COLUMN consent_audit_trail.additional_details IS 'JSON field for flexible additional data';

-- =====================================================
-- 4. UPDATE TRIGGERS
-- =====================================================

-- Trigger for consents table
CREATE TRIGGER trigger_consents_updated_at
    BEFORE UPDATE ON consents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for data_breach_logs table
CREATE TRIGGER trigger_data_breach_logs_updated_at
    BEFORE UPDATE ON data_breach_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for consent_audit_trail table
CREATE TRIGGER trigger_consent_audit_updated_at
    BEFORE UPDATE ON consent_audit_trail
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 5. AUDIT TRIGGER FOR CONSENTS
-- =====================================================
-- Automatically log all consent changes to audit trail

CREATE OR REPLACE FUNCTION log_consent_action()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO consent_audit_trail
            (consent_id, tenant_id, patient_id, action, new_status, action_by)
        VALUES
            (NEW.id, NEW.tenant_id, NEW.patient_id, 'GRANTED', NEW.status, NEW.recorded_by);
        RETURN NEW;

    ELSIF (TG_OP = 'UPDATE') THEN
        -- Log status changes
        IF OLD.status != NEW.status THEN
            INSERT INTO consent_audit_trail
                (consent_id, tenant_id, patient_id, action, previous_status, new_status,
                 reason, action_by)
            VALUES
                (NEW.id, NEW.tenant_id, NEW.patient_id,
                 CASE
                     WHEN NEW.status = 'WITHDRAWN' THEN 'WITHDRAWN'
                     WHEN NEW.status = 'EXPIRED' THEN 'EXPIRED'
                     WHEN NEW.status = 'SUPERSEDED' THEN 'RENEWED'
                     ELSE 'MODIFIED'
                 END,
                 OLD.status, NEW.status, NEW.withdrawal_reason, NEW.recorded_by);
        END IF;
        RETURN NEW;

    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO consent_audit_trail
            (consent_id, tenant_id, patient_id, action, previous_status, action_by)
        VALUES
            (OLD.id, OLD.tenant_id, OLD.patient_id, 'DELETED', OLD.status, NULL);
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply audit trigger to consents table
CREATE TRIGGER trigger_consent_audit
    AFTER INSERT OR UPDATE OR DELETE ON consents
    FOR EACH ROW
    EXECUTE FUNCTION log_consent_action();

-- =====================================================
-- 6. HELPER FUNCTIONS FOR COMPLIANCE
-- =====================================================

-- Function to check if patient has valid consent
CREATE OR REPLACE FUNCTION has_valid_consent(
    p_patient_id UUID,
    p_tenant_id VARCHAR,
    p_purpose VARCHAR
)
RETURNS BOOLEAN AS $$
DECLARE
    consent_exists BOOLEAN;
BEGIN
    SELECT EXISTS(
        SELECT 1
        FROM consents
        WHERE patient_id = p_patient_id
          AND tenant_id = p_tenant_id
          AND purpose = p_purpose
          AND status = 'ACTIVE'
          AND withdrawn_at IS NULL
          AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
    ) INTO consent_exists;

    RETURN consent_exists;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION has_valid_consent(UUID, VARCHAR, VARCHAR) IS
    'Check if patient has valid consent for a specific purpose';

-- Function to get consent summary for a patient
CREATE OR REPLACE FUNCTION get_patient_consent_summary(
    p_patient_id UUID,
    p_tenant_id VARCHAR
)
RETURNS TABLE(
    purpose VARCHAR,
    status VARCHAR,
    granted_at TIMESTAMP,
    expires_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT c.purpose, c.status, c.granted_at, c.expires_at
    FROM consents c
    WHERE c.patient_id = p_patient_id
      AND c.tenant_id = p_tenant_id
      AND c.status = 'ACTIVE'
      AND c.withdrawn_at IS NULL
    ORDER BY c.granted_at DESC;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_patient_consent_summary(UUID, VARCHAR) IS
    'Get summary of all active consents for a patient';

-- =====================================================
-- 7. ROW-LEVEL SECURITY FOR COMPLIANCE TABLES
-- =====================================================

-- Enable RLS on compliance tables
ALTER TABLE consents ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_breach_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE consent_audit_trail ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
CREATE POLICY tenant_isolation_policy ON consents
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON data_breach_logs
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy ON consent_audit_trail
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- =====================================================
-- 8. SCHEDULED MAINTENANCE FUNCTIONS
-- =====================================================

-- Function to mark expired consents (run daily)
CREATE OR REPLACE FUNCTION mark_expired_consents()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE consents
    SET status = 'EXPIRED'
    WHERE expires_at < CURRENT_TIMESTAMP
      AND status = 'ACTIVE';

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION mark_expired_consents() IS
    'Marks expired consents as EXPIRED (run daily via scheduler)';

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
DECLARE
    tables_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO tables_count
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename IN ('consents', 'data_breach_logs', 'consent_audit_trail');

    RAISE NOTICE 'Compliance tables created: %', tables_count;
END $$;
