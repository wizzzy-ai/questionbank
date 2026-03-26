package com.example.questionbank.web;

import com.example.questionbank.Question;

/**
 * Lightweight view-model holding per-question result data for the result page.
 */
public record QuestionResultItem(
        Question question,
        String   userAnswer,
        String   correctAnswer,
        boolean  correct
) {}
