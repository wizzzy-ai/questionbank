package com.example.questionbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "quiz_results")
public class QuizResult {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(nullable = false)
	private int score;

	@Column(nullable = false)
	private int totalQuestions;

	@Column(nullable = false)
	private double percentage;

	@Column(nullable = false, length = 20)
	private String mode = "ADAPTIVE";

	@Column(length = 20)
	private String difficultyBand;

	@Column(length = 255)
	private String topicSummary;

	@Column(nullable = false)
	private Integer durationSeconds = 0;

	@Column(nullable = false)
	private boolean completed = true;

	@Column(nullable = false)
	private Instant submittedAt = Instant.now();

	public QuizResult() {
	}

	public QuizResult(Student student, int score, int totalQuestions, double percentage) {
		this.student = student;
		this.score = score;
		this.totalQuestions = totalQuestions;
		this.percentage = percentage;
		this.submittedAt = Instant.now();
	}

	public QuizResult(
			Student student,
			int score,
			int totalQuestions,
			double percentage,
			String mode,
			String difficultyBand,
			String topicSummary,
			Integer durationSeconds
	) {
		this.student = student;
		this.score = score;
		this.totalQuestions = totalQuestions;
		this.percentage = percentage;
		this.mode = mode == null || mode.isBlank() ? "ADAPTIVE" : mode;
		this.difficultyBand = difficultyBand;
		this.topicSummary = topicSummary;
		this.durationSeconds = durationSeconds == null ? 0 : Math.max(0, durationSeconds);
		this.completed = true;
		this.submittedAt = Instant.now();
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

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getTotalQuestions() {
		return totalQuestions;
	}

	public void setTotalQuestions(int totalQuestions) {
		this.totalQuestions = totalQuestions;
	}

	public double getPercentage() {
		return percentage;
	}

	public void setPercentage(double percentage) {
		this.percentage = percentage;
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

	public Integer getDurationSeconds() {
		return durationSeconds == null ? 0 : durationSeconds;
	}

	public void setDurationSeconds(Integer durationSeconds) {
		this.durationSeconds = durationSeconds == null ? 0 : Math.max(0, durationSeconds);
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}
}

