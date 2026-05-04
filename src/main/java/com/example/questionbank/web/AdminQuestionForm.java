package com.example.questionbank.web;

import com.example.questionbank.QuestionDifficultyBand;
import com.example.questionbank.QuestionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminQuestionForm {
	private Long id;

	@NotBlank
	@Size(max = 2000)
	private String questionText;

	@NotBlank
	@Size(max = 500)
	private String optionA;

	@NotBlank
	@Size(max = 500)
	private String optionB;

	@NotBlank
	@Size(max = 500)
	private String optionC;

	@NotBlank
	@Size(max = 500)
	private String optionD;

	@NotBlank
	private String correctOption;

	private Long categoryId;

	private QuestionDifficultyBand difficultyBand = QuestionDifficultyBand.MEDIUM;

	private QuestionStatus status = QuestionStatus.ACTIVE;

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

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public QuestionDifficultyBand getDifficultyBand() {
		return difficultyBand;
	}

	public void setDifficultyBand(QuestionDifficultyBand difficultyBand) {
		this.difficultyBand = difficultyBand;
	}

	public QuestionStatus getStatus() {
		return status;
	}

	public void setStatus(QuestionStatus status) {
		this.status = status;
	}
}
