package com.example.questionbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "categories")
public class Category {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(length = 16)
	private String color;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public Category() {
	}

	public Category(String name) {
		setName(name);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = normalizeName(name);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = normalizeOptionalText(description);
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = normalizeOptionalText(color);
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	@PrePersist
	void ensureDefaults() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		name = normalizeName(name);
		description = normalizeOptionalText(description);
		color = normalizeOptionalText(color);
	}

	private static String normalizeName(String name) {
		return name == null || name.isBlank() ? "General" : name.trim();
	}

	private static String normalizeOptionalText(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
