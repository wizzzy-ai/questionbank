package com.example.questionbank.web;

import com.example.questionbank.Question;
import com.example.questionbank.QuestionDifficultyBand;

import java.util.ArrayList;
import java.util.List;

public record QuizQuestionView(
		Long id,
		String questionText,
		String category,
		QuestionDifficultyBand difficulty,
		List<QuizOptionView> options
) {
	public static QuizQuestionView from(Question question) {
		List<QuizOptionView> ordered = new ArrayList<>(4);
		ordered.add(new QuizOptionView("A", question.getOptionA()));
		ordered.add(new QuizOptionView("B", question.getOptionB()));
		ordered.add(new QuizOptionView("C", question.getOptionC()));
		ordered.add(new QuizOptionView("D", question.getOptionD()));

		return new QuizQuestionView(
				question.getId(),
				question.getQuestionText(),
				question.getCategory(),
				question.getDifficultyBand(),
				List.copyOf(ordered)
		);
	}
}
