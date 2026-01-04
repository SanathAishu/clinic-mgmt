-- Add missing columns to users table

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS metadata TEXT;

-- Update role column to be consistent
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50);
