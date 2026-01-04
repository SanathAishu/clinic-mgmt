-- Patient Service Initial Schema
-- Creates patients table

CREATE TABLE IF NOT EXISTS patients (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    gender VARCHAR(10) NOT NULL,
    date_of_birth DATE NOT NULL,
    address VARCHAR(500),
    disease VARCHAR(50) NOT NULL,
    medical_history TEXT,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Indexes for performance
CREATE INDEX idx_patients_user_id ON patients(user_id);
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_disease ON patients(disease);
CREATE INDEX idx_patients_active ON patients(active);
CREATE INDEX idx_patients_created_at ON patients(created_at);

-- Comments for documentation
COMMENT ON TABLE patients IS 'Patient demographic and medical information';
COMMENT ON COLUMN patients.id IS 'Unique identifier for the patient (UUID)';
COMMENT ON COLUMN patients.user_id IS 'Reference to user in auth-service';
COMMENT ON COLUMN patients.disease IS 'Primary disease/condition';
COMMENT ON COLUMN patients.medical_history IS 'Patient medical history notes';
COMMENT ON COLUMN patients.metadata IS 'Additional flexible data storage (JSON)';
