-- Add description column to rooms table
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS description TEXT;
