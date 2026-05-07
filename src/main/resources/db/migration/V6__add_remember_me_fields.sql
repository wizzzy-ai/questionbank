-- Add remember-me functionality fields to students table
ALTER TABLE students 
ADD COLUMN remember_me_token VARCHAR(120) NULL,
ADD COLUMN remember_me_expires_at TIMESTAMP NULL;

-- Add index for faster lookups
CREATE INDEX idx_students_remember_me_token ON students(remember_me_token);
