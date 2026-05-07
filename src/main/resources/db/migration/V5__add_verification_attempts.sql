-- Add verification_attempts column to students table for OTP security
ALTER TABLE students
ADD COLUMN verification_attempts INT NOT NULL DEFAULT 0
AFTER verification_token_expires_at;
