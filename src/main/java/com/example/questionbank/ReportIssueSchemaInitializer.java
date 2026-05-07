package com.example.questionbank;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportIssueSchemaInitializer implements ApplicationRunner {
	private final JdbcTemplate jdbcTemplate;

	public ReportIssueSchemaInitializer(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		ensureTableExists();
		ensureColumnExists("report_issues", "user_id", "BIGINT NOT NULL");
		ensureColumnExists("report_issues", "message", "VARCHAR(500) NOT NULL");
		ensureColumnExists("report_issues", "status", "VARCHAR(20) NOT NULL DEFAULT 'OPEN'");
		ensureColumnExists("report_issues", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
	}

	private void ensureTableExists() {
		Integer count = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name = 'report_issues'
				""",
				Integer.class);
		if (count == null || count == 0) {
			jdbcTemplate.execute("""
					CREATE TABLE report_issues (
						id BIGINT AUTO_INCREMENT PRIMARY KEY,
						user_id BIGINT NOT NULL,
						message VARCHAR(500) NOT NULL,
						status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
						created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
						INDEX idx_status (status),
						INDEX idx_created_at (created_at)
					)
					""");
		}
	}

	private void ensureColumnExists(String tableName, String columnName, String columnDefinition) {
		Integer count = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = ?
				  AND column_name = ?
				""",
				Integer.class,
				tableName,
				columnName);
		if (count == null || count == 0) {
			jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
		}
	}
}
