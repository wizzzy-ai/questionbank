package com.example.questionbank;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaPatchBootstrap {
	private final JdbcTemplate jdbcTemplate;

	public SchemaPatchBootstrap(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Order(-100)
	@EventListener(ApplicationReadyEvent.class)
	public void ensureAdminColumns() {
		ensureCategoriesTable();
		addQuestionColumns();
		addStudentColumns();
		backfillQuestionCategories();
	}

	private void ensureCategoriesTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS categories (
				    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
				    name VARCHAR(80) NOT NULL UNIQUE,
				    description VARCHAR(500) NULL,
				    color VARCHAR(16) NULL,
				    created_at DATETIME NULL
				)
				""");
		addColumnIfMissing("categories", "description", "ALTER TABLE categories ADD COLUMN description VARCHAR(500) NULL");
		addColumnIfMissing("categories", "color", "ALTER TABLE categories ADD COLUMN color VARCHAR(16) NULL");
		jdbcTemplate.execute("INSERT IGNORE INTO categories (name, created_at) VALUES ('General', CURRENT_TIMESTAMP)");
		jdbcTemplate.execute("""
				INSERT IGNORE INTO categories (name, created_at)
				SELECT DISTINCT TRIM(category), CURRENT_TIMESTAMP
				FROM questions
				WHERE category IS NOT NULL
				  AND TRIM(category) <> ''
				""");
		jdbcTemplate.execute("UPDATE categories SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
	}

	private void addQuestionColumns() {
		addColumnIfMissing("questions", "status", "ALTER TABLE questions ADD COLUMN status VARCHAR(20) NULL");
		addColumnIfMissing("questions", "created_at", "ALTER TABLE questions ADD COLUMN created_at DATETIME NULL");
		addColumnIfMissing("questions", "category_id", "ALTER TABLE questions ADD COLUMN category_id BIGINT NULL");
		jdbcTemplate.execute("UPDATE questions SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");
		jdbcTemplate.execute("UPDATE questions SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
	}

	private void addStudentColumns() {
		addColumnIfMissing("students", "banned", "ALTER TABLE students ADD COLUMN banned BIT NULL");
		addColumnIfMissing("students", "created_at", "ALTER TABLE students ADD COLUMN created_at DATETIME NULL");
		addColumnIfMissing("students", "verification_attempts", "ALTER TABLE students ADD COLUMN verification_attempts INT NOT NULL DEFAULT 0");
		jdbcTemplate.execute("UPDATE students SET banned = b'0' WHERE banned IS NULL");
		jdbcTemplate.execute("UPDATE students SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
	}

	private void backfillQuestionCategories() {
		jdbcTemplate.execute("""
				UPDATE questions q
				JOIN categories c
				  ON c.name = COALESCE(NULLIF(TRIM(q.category), ''), 'General')
				SET q.category_id = c.id
				WHERE q.category_id IS NULL
				""");
	}

	private void addColumnIfMissing(String tableName, String columnName, String ddl) {
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
				columnName
		);
		if (count == null || count == 0) {
			jdbcTemplate.execute(ddl);
		}
	}
}
