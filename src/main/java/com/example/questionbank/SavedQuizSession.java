package com.example.questionbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "saved_quiz_sessions")
public class SavedQuizSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String questionIdsJson = "[]";

	@Column(nullable = false, columnDefinition = "TEXT")
	private String answersJson = "{}";

	@Column(nullable = false)
	private int totalQuestions;

	@Column(nullable = false)
	private int secondsRemaining;

	@Column(nullable = false, length = 20)
	private String mode = "ADAPTIVE";

	@Column(length = 20)
	private String difficultyBand;

	@Column(length = 255)
	private String topicSummary;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	public SavedQuizSession() {
	}

	public SavedQuizSession(Student student, String questionIdsJson, int totalQuestions, int secondsRemaining, String mode, String difficultyBand, String topicSummary) {
		this.student = student;
		this.questionIdsJson = questionIdsJson == null || questionIdsJson.isBlank() ? "[]" : questionIdsJson;
		this.totalQuestions = totalQuestions;
		this.secondsRemaining = Math.max(0, secondsRemaining);
		this.mode = mode == null || mode.isBlank() ? "ADAPTIVE" : mode;
		this.difficultyBand = difficultyBand;
		this.topicSummary = topicSummary;
		this.answersJson = "{}";
		this.active = true;
	}

	@PrePersist
	@PreUpdate
	void touch() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public String getQuestionIdsJson() {
		return questionIdsJson;
	}

	public void setQuestionIdsJson(String questionIdsJson) {
		this.questionIdsJson = questionIdsJson == null || questionIdsJson.isBlank() ? "[]" : questionIdsJson;
	}

	public String getAnswersJson() {
		return answersJson;
	}

	public void setAnswersJson(String answersJson) {
		this.answersJson = answersJson == null || answersJson.isBlank() ? "{}" : answersJson;
	}

	public int getTotalQuestions() {
		return totalQuestions;
	}

	public void setTotalQuestions(int totalQuestions) {
		this.totalQuestions = Math.max(0, totalQuestions);
	}

	public int getSecondsRemaining() {
		return Math.max(0, secondsRemaining);
	}

	public void setSecondsRemaining(int secondsRemaining) {
		this.secondsRemaining = Math.max(0, secondsRemaining);
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode == null || mode.isBlank() ? "ADAPTIVE" : mode;
	}

	public String getDifficultyBand() {
		return difficultyBand;
	}

	public void setDifficultyBand(String difficultyBand) {
		this.difficultyBand = difficultyBand;
	}

	public String getTopicSummary() {
		return topicSummary;
	}

	public void setTopicSummary(String topicSummary) {
		this.topicSummary = topicSummary;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
