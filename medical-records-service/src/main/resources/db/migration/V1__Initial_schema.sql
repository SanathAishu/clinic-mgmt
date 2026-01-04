-- Medical Records Service Initial Schema
-- Creates medical_records, prescriptions, and medical_reports tables

-- Medical Records table
CREATE TABLE IF NOT EXISTS medical_records (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    record_date DATE NOT NULL,
    diagnosis TEXT NOT NULL,
    symptoms TEXT,
    treatment TEXT,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Prescriptions table
CREATE TABLE IF NOT EXISTS prescriptions (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    medical_record_id UUID,
    prescription_date DATE NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    instructions TEXT,
    refillable BOOLEAN DEFAULT false,
    refills_remaining INTEGER DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Medical Reports table
CREATE TABLE IF NOT EXISTS medical_reports (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    medical_record_id UUID,
    report_type VARCHAR(100),
    report_date DATE NOT NULL,
    report_title VARCHAR(255),
    report_content TEXT,
    file_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Indexes for performance
CREATE INDEX idx_medical_records_patient_id ON medical_records(patient_id);
CREATE INDEX idx_medical_records_doctor_id ON medical_records(doctor_id);
CREATE INDEX idx_medical_records_record_date ON medical_records(record_date);
CREATE INDEX idx_medical_records_active ON medical_records(active);

CREATE INDEX idx_prescriptions_patient_id ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_doctor_id ON prescriptions(doctor_id);
CREATE INDEX idx_prescriptions_medical_record_id ON prescriptions(medical_record_id);
CREATE INDEX idx_prescriptions_prescription_date ON prescriptions(prescription_date);
CREATE INDEX idx_prescriptions_active ON prescriptions(active);
CREATE INDEX idx_prescriptions_refillable ON prescriptions(refillable);

CREATE INDEX idx_medical_reports_patient_id ON medical_reports(patient_id);
CREATE INDEX idx_medical_reports_doctor_id ON medical_reports(doctor_id);
CREATE INDEX idx_medical_reports_medical_record_id ON medical_reports(medical_record_id);
CREATE INDEX idx_medical_reports_report_type ON medical_reports(report_type);
CREATE INDEX idx_medical_reports_report_date ON medical_reports(report_date);
CREATE INDEX idx_medical_reports_active ON medical_reports(active);

-- Comments for documentation
COMMENT ON TABLE medical_records IS 'Patient medical records and diagnoses';
COMMENT ON TABLE prescriptions IS 'Medication prescriptions issued to patients';
COMMENT ON TABLE medical_reports IS 'Medical test reports and imaging results';

COMMENT ON COLUMN medical_records.patient_id IS 'UUID reference to patient (no FK)';
COMMENT ON COLUMN medical_records.doctor_id IS 'UUID reference to doctor (no FK)';
COMMENT ON COLUMN prescriptions.refillable IS 'Whether prescription can be refilled';
COMMENT ON COLUMN prescriptions.refills_remaining IS 'Number of refills remaining';
COMMENT ON COLUMN medical_reports.report_type IS 'Type of report (LAB, XRAY, MRI, etc.)';
COMMENT ON COLUMN medical_reports.file_url IS 'URL to stored report file';
