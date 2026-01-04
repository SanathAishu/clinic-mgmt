-- Doctor Service Initial Schema
-- Creates doctors table

CREATE TABLE IF NOT EXISTS doctors (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    gender VARCHAR(10) NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    license_number VARCHAR(50),
    years_of_experience INTEGER NOT NULL,
    qualifications VARCHAR(500),
    biography TEXT,
    clinic_address VARCHAR(100),
    consultation_fee VARCHAR(20),
    available BOOLEAN NOT NULL DEFAULT true,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Indexes for performance
CREATE INDEX idx_doctors_user_id ON doctors(user_id);
CREATE INDEX idx_doctors_email ON doctors(email);
CREATE INDEX idx_doctors_specialty ON doctors(specialty);
CREATE INDEX idx_doctors_license_number ON doctors(license_number);
CREATE INDEX idx_doctors_available ON doctors(available);
CREATE INDEX idx_doctors_active ON doctors(active);
CREATE INDEX idx_doctors_created_at ON doctors(created_at);

-- Comments for documentation
COMMENT ON TABLE doctors IS 'Doctor professional information and availability';
COMMENT ON COLUMN doctors.id IS 'Unique identifier for the doctor (UUID)';
COMMENT ON COLUMN doctors.user_id IS 'Reference to user in auth-service';
COMMENT ON COLUMN doctors.specialty IS 'Medical specialty (e.g., CARDIOLOGY, NEUROLOGY)';
COMMENT ON COLUMN doctors.license_number IS 'Medical license number';
COMMENT ON COLUMN doctors.available IS 'Whether doctor is accepting new patients';
COMMENT ON COLUMN doctors.metadata IS 'Additional data (working hours, languages, etc.)';
