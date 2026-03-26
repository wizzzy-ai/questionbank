package com.example.questionbank;

public enum QuestionDifficultyBand {
	EASY,
	MEDIUM,
	HARD;

	public static QuestionDifficultyBand fromEstimatedCorrectRate(double estimatedCorrectRate) {
		if (estimatedCorrectRate >= 0.75) {
			return EASY;
		}
		if (estimatedCorrectRate <= 0.45) {
			return HARD;
		}
		return MEDIUM;
	}
}

