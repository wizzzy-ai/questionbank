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
@Table(name = "quiz_answers")
public class QuizAnswer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_result_id", nullable = false)
	private QuizResult quizResult;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(length = 1)
	private String userAnswer;

	@Column(nullable = false, length = 1)
	private String correctAnswer;

	@Column(nullable = false)
	private boolean correct;

	@Column(nullable = false, length = 80)
	private String category;

	@Column(nullable = false)
	private Instant answeredAt = Instant.now();

	public QuizAnswer() {
	}

	public QuizAnswer(Student student, QuizResult quizResult, Question question, String userAnswer, String correctAnswer, boolean correct, String category, Instant answeredAt) {
		this.student = student;
		this.quizResult = quizResult;
		this.question = question;
		this.userAnswer = userAnswer;
		this.correctAnswer = correctAnswer;
		this.correct = correct;
		this.category = (category == null || category.isBlank()) ? "General" : category;
		this.answeredAt = answeredAt == null ? Instant.now() : answeredAt;
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

	public QuizResult getQuizResult() {
		return quizResult;
	}

	public void setQuizResult(QuizResult quizResult) {
		this.quizResult = quizResult;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

	public String getUserAnswer() {
		return userAnswer;
	}

	public void setUserAnswer(String userAnswer) {
		this.userAnswer = userAnswer;
	}

	public String getCorrectAnswer() {
		return correctAnswer;
	}

	public void setCorrectAnswer(String correctAnswer) {
		this.correctAnswer = correctAnswer;
	}

	public boolean isCorrect() {
		return correct;
	}

	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = (category == null || category.isBlank()) ? "General" : category;
	}

	public Instant getAnsweredAt() {
		return answeredAt;
	}

	public void setAnsweredAt(Instant answeredAt) {
		this.answeredAt = answeredAt;
	}
}

