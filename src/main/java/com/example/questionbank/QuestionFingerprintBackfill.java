package com.example.questionbank;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionFingerprintBackfill {
	private final QuestionRepository questionRepository;

	public QuestionFingerprintBackfill(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Order(0)
	@EventListener(ApplicationReadyEvent.class)
	public void backfillFingerprints() {
		for (int i = 0; i < 20; i++) {
			List<Question> batch = questionRepository.findTop500ByFingerprintIsNull();
			if (batch.isEmpty()) {
				return;
			}
			for (Question q : batch) {
				q.setFingerprint(QuestionFingerprint.compute(q));
			}
			questionRepository.saveAll(batch);
		}
	}
}

