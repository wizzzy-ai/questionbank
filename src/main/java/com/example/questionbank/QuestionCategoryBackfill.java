package com.example.questionbank;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionCategoryBackfill {
	private final QuestionRepository questionRepository;
	private final CategoryRepository categoryRepository;

	public QuestionCategoryBackfill(QuestionRepository questionRepository, CategoryRepository categoryRepository) {
		this.questionRepository = questionRepository;
		this.categoryRepository = categoryRepository;
	}

	@Order(30)
	@EventListener(ApplicationReadyEvent.class)
	public void backfillQuestionCategories() {
		for (int i = 0; i < 20; i++) {
			List<Question> batch = questionRepository.findTop500ByCategoryEntityIsNull();
			if (batch.isEmpty()) {
				return;
			}
			for (Question question : batch) {
				question.setCategoryEntity(resolveCategory(question.getCategory()));
			}
			questionRepository.saveAll(batch);
		}
	}

	private Category resolveCategory(String name) {
		String normalized = name == null || name.isBlank() ? "General" : name.trim();
		return categoryRepository.findByNameIgnoreCase(normalized)
				.orElseGet(() -> categoryRepository.save(new Category(normalized)));
	}
}
