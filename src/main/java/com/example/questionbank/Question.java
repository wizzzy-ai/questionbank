package com.example.questionbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(nullable = false, length = 2000)
	    private String questionText;

	    @Column(nullable = false, length = 500)
	    private String optionA;

	    @Column(nullable = false, length = 500)
	    private String optionB;

	    @Column(nullable = false, length = 500)
	    private String optionC;

	    @Column(nullable = false, length = 500)
	    private String optionD;

	    @Column(nullable = false, length = 1)
	    private String correctOption;

		    @Column(name = "category", nullable = false, length = 80)
		    private String categoryText = "General";

			@ManyToOne
			@JoinColumn(name = "category_id")
			private Category categoryEntity;

			@Column(length = 64)
			private String fingerprint;

			@Column(name = "attempts_count")
			private Integer attemptsCount = 0;

			@Column(name = "correct_count")
			private Integer correctCount = 0;

			@Enumerated(EnumType.STRING)
			@Column(name = "difficulty_band", length = 10)
			private QuestionDifficultyBand difficultyBand = QuestionDifficultyBand.MEDIUM;

			@Enumerated(EnumType.STRING)
			@Column(nullable = false, length = 20)
			private QuestionStatus status = QuestionStatus.ACTIVE;

			@Column(name = "created_at", nullable = false)
			private Instant createdAt;

		    public Question() {}

	    public Question(
			    String questionText,
			    String optionA,
			    String optionB,
			    String optionC,
			    String optionD,
			    String correctOption,
			    String category
	    ) {
	        this.questionText = questionText;
	        this.optionA = optionA;
	        this.optionB = optionB;
	        this.optionC = optionC;
	        this.optionD = optionD;
	        this.correctOption = correctOption;
	        this.categoryText = (category == null || category.isBlank()) ? "General" : category.trim();
	    }

	    public Long getId() {
	        return id;
	    }

	    public void setId(Long id) {
	        this.id = id;
	    }

	    public String getQuestionText() {
	        return questionText;
	    }

	    public void setQuestionText(String questionText) {
	        this.questionText = questionText;
	    }

	    public String getOptionA() {
	        return optionA;
	    }

	    public void setOptionA(String optionA) {
	        this.optionA = optionA;
	    }

	    public String getOptionB() {
	        return optionB;
	    }

	    public void setOptionB(String optionB) {
	        this.optionB = optionB;
	    }

	    public String getOptionC() {
	        return optionC;
	    }

	    public void setOptionC(String optionC) {
	        this.optionC = optionC;
	    }

	    public String getOptionD() {
	        return optionD;
	    }

	    public void setOptionD(String optionD) {
	        this.optionD = optionD;
	    }

	    public String getCorrectOption() {
	        return correctOption;
	    }

	    public void setCorrectOption(String correctOption) {
	        this.correctOption = correctOption;
	    }

	    public String getCategory() {
	        if (categoryEntity != null && categoryEntity.getName() != null && !categoryEntity.getName().isBlank()) {
	        	return categoryEntity.getName();
	        }
	        return categoryText == null || categoryText.isBlank() ? "General" : categoryText;
	    }

		    public void setCategory(String category) {
		        this.categoryText = (category == null || category.isBlank()) ? "General" : category.trim();
		    }

			public Category getCategoryEntity() {
				return categoryEntity;
			}

			public void setCategoryEntity(Category categoryEntity) {
				this.categoryEntity = categoryEntity;
				this.categoryText = categoryEntity == null ? getCategory() : categoryEntity.getName();
			}

			public String getFingerprint() {
				return fingerprint;
			}

			public void setFingerprint(String fingerprint) {
				this.fingerprint = fingerprint;
			}

			public Integer getAttemptsCount() {
				return attemptsCount == null ? 0 : attemptsCount;
			}

			public void setAttemptsCount(Integer attemptsCount) {
				this.attemptsCount = attemptsCount == null ? 0 : attemptsCount;
			}

			public Integer getCorrectCount() {
				return correctCount == null ? 0 : correctCount;
			}

			public void setCorrectCount(Integer correctCount) {
				this.correctCount = correctCount == null ? 0 : correctCount;
			}

			public QuestionDifficultyBand getDifficultyBand() {
				return difficultyBand == null ? QuestionDifficultyBand.MEDIUM : difficultyBand;
			}

			public void setDifficultyBand(QuestionDifficultyBand difficultyBand) {
				this.difficultyBand = difficultyBand == null ? QuestionDifficultyBand.MEDIUM : difficultyBand;
			}

			public QuestionStatus getStatus() {
				return status == null ? QuestionStatus.ACTIVE : status;
			}

			public void setStatus(QuestionStatus status) {
				this.status = status == null ? QuestionStatus.ACTIVE : status;
			}

			public Instant getCreatedAt() {
				return createdAt;
			}

			public void setCreatedAt(Instant createdAt) {
				this.createdAt = createdAt;
			}

			public void recordAttempt(boolean wasCorrect) {
				int attempts = getAttemptsCount() + 1;
				int correct = getCorrectCount() + (wasCorrect ? 1 : 0);
				this.attemptsCount = attempts;
				this.correctCount = correct;

				double estimatedCorrectRate = (correct + 1.0) / (attempts + 2.0);
				this.difficultyBand = QuestionDifficultyBand.fromEstimatedCorrectRate(estimatedCorrectRate);
			}

			@PrePersist
			@PreUpdate
			void ensureFingerprint() {
				if (createdAt == null) {
					createdAt = Instant.now();
				}
				if (categoryEntity != null && categoryEntity.getName() != null && !categoryEntity.getName().isBlank()) {
					categoryText = categoryEntity.getName();
				}
				if (categoryText == null || categoryText.isBlank()) {
					categoryText = "General";
				}
				if (fingerprint == null || fingerprint.isBlank()) {
					fingerprint = QuestionFingerprint.compute(this);
				}
			}
	}
