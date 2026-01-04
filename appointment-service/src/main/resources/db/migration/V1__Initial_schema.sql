-- Appointment Service Initial Schema
-- Creates appointments, patient_snapshots, and doctor_snapshots tables

-- Patient snapshots (denormalized patient data)
CREATE TABLE IF NOT EXISTS patient_snapshots (
    patient_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    gender VARCHAR(10) NOT NULL,
    disease VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP
);

-- Doctor snapshots (denormalized doctor data)
CREATE TABLE IF NOT EXISTS doctor_snapshots (
    doctor_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    gender VARCHAR(10) NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP
);

-- Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    appointment_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Indexes for performance
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX idx_appointments_appointment_date ON appointments(appointment_date);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_created_at ON appointments(created_at);

CREATE INDEX idx_patient_snapshots_email ON patient_snapshots(email);
CREATE INDEX idx_doctor_snapshots_email ON doctor_snapshots(email);
CREATE INDEX idx_doctor_snapshots_specialty ON doctor_snapshots(specialty);

-- Comments for documentation
COMMENT ON TABLE appointments IS 'Patient-doctor appointment bookings';
COMMENT ON TABLE patient_snapshots IS 'Denormalized patient data for fast queries';
COMMENT ON TABLE doctor_snapshots IS 'Denormalized doctor data for fast queries';

COMMENT ON COLUMN appointments.patient_id IS 'UUID reference to patient (no FK constraint)';
COMMENT ON COLUMN appointments.doctor_id IS 'UUID reference to doctor (no FK constraint)';
COMMENT ON COLUMN appointments.status IS 'PENDING, CONFIRMED, CANCELLED, COMPLETED';
COMMENT ON COLUMN appointments.metadata IS 'Additional appointment data (JSON)';

COMMENT ON COLUMN patient_snapshots.patient_id IS 'Same as patient.id in patient-service';
COMMENT ON COLUMN doctor_snapshots.doctor_id IS 'Same as doctor.id in doctor-service';
