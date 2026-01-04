-- Auth Service Initial Schema
-- Creates users table for authentication

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

-- Comments for documentation
COMMENT ON TABLE users IS 'User authentication and authorization table';
COMMENT ON COLUMN users.id IS 'Unique identifier for the user (UUID)';
COMMENT ON COLUMN users.email IS 'User email address (unique, used for login)';
COMMENT ON COLUMN users.password IS 'Bcrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role: ADMIN, DOCTOR, NURSE, RECEPTIONIST, PATIENT';
COMMENT ON COLUMN users.active IS 'Whether the user account is active';
