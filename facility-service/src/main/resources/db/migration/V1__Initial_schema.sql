-- Facility Service Initial Schema
-- Creates rooms and room_bookings tables

-- Rooms table
CREATE TABLE IF NOT EXISTS rooms (
    id UUID PRIMARY KEY,
    room_number VARCHAR(50) NOT NULL UNIQUE,
    room_type VARCHAR(50) NOT NULL,
    floor VARCHAR(20),
    wing VARCHAR(50),
    capacity INTEGER NOT NULL DEFAULT 1,
    current_occupancy INTEGER NOT NULL DEFAULT 0,
    available BOOLEAN NOT NULL DEFAULT true,
    active BOOLEAN NOT NULL DEFAULT true,
    amenities TEXT,
    daily_rate DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Room Bookings table
CREATE TABLE IF NOT EXISTS room_bookings (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    admission_date DATE NOT NULL,
    expected_discharge_date DATE,
    actual_discharge_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    metadata TEXT
);

-- Indexes for performance
CREATE INDEX idx_rooms_room_number ON rooms(room_number);
CREATE INDEX idx_rooms_room_type ON rooms(room_type);
CREATE INDEX idx_rooms_floor ON rooms(floor);
CREATE INDEX idx_rooms_wing ON rooms(wing);
CREATE INDEX idx_rooms_available ON rooms(available);
CREATE INDEX idx_rooms_active ON rooms(active);

CREATE INDEX idx_room_bookings_room_id ON room_bookings(room_id);
CREATE INDEX idx_room_bookings_patient_id ON room_bookings(patient_id);
CREATE INDEX idx_room_bookings_admission_date ON room_bookings(admission_date);
CREATE INDEX idx_room_bookings_status ON room_bookings(status);
CREATE INDEX idx_room_bookings_created_at ON room_bookings(created_at);

-- Comments for documentation
COMMENT ON TABLE rooms IS 'Hospital room inventory and availability';
COMMENT ON TABLE room_bookings IS 'Patient room admission and booking records';

COMMENT ON COLUMN rooms.room_type IS 'GENERAL, PRIVATE, ICU, EMERGENCY, etc.';
COMMENT ON COLUMN rooms.capacity IS 'Maximum number of beds in the room';
COMMENT ON COLUMN rooms.current_occupancy IS 'Current number of patients in room';
COMMENT ON COLUMN rooms.amenities IS 'List of room amenities (JSON)';
COMMENT ON COLUMN rooms.daily_rate IS 'Daily charge for the room';

COMMENT ON COLUMN room_bookings.room_id IS 'UUID reference to room (no FK)';
COMMENT ON COLUMN room_bookings.patient_id IS 'UUID reference to patient (no FK)';
COMMENT ON COLUMN room_bookings.status IS 'PENDING, CONFIRMED, CANCELLED, DISCHARGED';
