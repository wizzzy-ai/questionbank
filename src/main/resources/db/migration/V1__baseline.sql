CREATE TABLE IF NOT EXISTS students (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    total_points INT NOT NULL DEFAULT 0,
    leaderboard_visible BIT NOT NULL DEFAULT b'1',
    admin BIT NOT NULL DEFAULT b'0',
    email_verified BIT NOT NULL DEFAULT b'1',
    verification_token VARCHAR(120) NULL,
    verification_token_expires_at TIMESTAMP NULL,
    password_reset_token VARCHAR(120) NULL,
    password_reset_token_expires_at TIMESTAMP NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    lock_until TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_text VARCHAR(2000) NOT NULL,
    option_a VARCHAR(500) NOT NULL,
    option_b VARCHAR(500) NOT NULL,
    option_c VARCHAR(500) NOT NULL,
    option_d VARCHAR(500) NOT NULL,
    correct_option VARCHAR(1) NOT NULL,
    category VARCHAR(80) NOT NULL,
    fingerprint VARCHAR(64) NULL,
    attempts_count INT NULL DEFAULT 0,
    correct_count INT NULL DEFAULT 0,
    difficulty_band VARCHAR(10) NULL
);

CREATE TABLE IF NOT EXISTS quiz_results (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    percentage DOUBLE NOT NULL,
    mode VARCHAR(20) NOT NULL DEFAULT 'ADAPTIVE',
    difficulty_band VARCHAR(20) NULL,
    topic_summary VARCHAR(255) NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    completed BIT NOT NULL DEFAULT b'1',
    submitted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_quiz_results_student FOREIGN KEY (student_id) REFERENCES students(id)
);

CREATE TABLE IF NOT EXISTS quiz_answers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    quiz_result_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer VARCHAR(1) NULL,
    correct_answer VARCHAR(1) NOT NULL,
    correct BIT NOT NULL,
    category VARCHAR(80) NOT NULL,
    answered_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_quiz_answers_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_quiz_answers_result FOREIGN KEY (quiz_result_id) REFERENCES quiz_results(id),
    CONSTRAINT fk_quiz_answers_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS bookmarks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_bookmarks_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_bookmarks_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uk_bookmarks_student_question UNIQUE (student_id, question_id)
);

CREATE TABLE IF NOT EXISTS saved_quiz_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    question_ids_json TEXT NOT NULL,
    answers_json TEXT NOT NULL,
    total_questions INT NOT NULL,
    seconds_remaining INT NOT NULL,
    mode VARCHAR(20) NOT NULL DEFAULT 'ADAPTIVE',
    difficulty_band VARCHAR(20) NULL,
    topic_summary VARCHAR(255) NULL,
    active BIT NOT NULL DEFAULT b'1',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_saved_quiz_sessions_student FOREIGN KEY (student_id) REFERENCES students(id)
);
