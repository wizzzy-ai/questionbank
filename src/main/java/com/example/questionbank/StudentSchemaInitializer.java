package com.example.questionbank;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudentSchemaInitializer implements ApplicationRunner {
	private final JdbcTemplate jdbcTemplate;

	public StudentSchemaInitializer(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		ensureStudentsColumn("banned",
				"ALTER TABLE students ADD COLUMN banned BIT NOT NULL DEFAULT b'0'");
		ensureStudentsColumn("created_at",
				"ALTER TABLE students ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
		ensureStudentsColumn("deleted",
				"ALTER TABLE students ADD COLUMN deleted BIT NOT NULL DEFAULT b'0'");
		ensureStudentsColumn("deleted_at",
				"ALTER TABLE students ADD COLUMN deleted_at TIMESTAMP NULL");
		ensureStudentsColumn("role",
				"ALTER TABLE students ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'");
		ensureStudentsColumn("bootstrap_admin",
				"ALTER TABLE students ADD COLUMN bootstrap_admin BIT NOT NULL DEFAULT b'0'");
		ensureStudentsColumn("last_login_at",
				"ALTER TABLE students ADD COLUMN last_login_at TIMESTAMP NULL");
		ensureStudentsColumn("verification_attempts",
				"ALTER TABLE students ADD COLUMN verification_attempts INT NOT NULL DEFAULT 0");
	}

	private void ensureStudentsColumn(String columnName, String alterSql) {
		Integer count = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = 'students'
				  AND column_name = ?
				""",
				Integer.class,
				columnName);
		if (count == null || count == 0) {
			jdbcTemplate.execute(alterSql);
		}
	}
}
