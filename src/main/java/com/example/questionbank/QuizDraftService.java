package com.example.questionbank;

import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuizDraftService {
	private final SavedQuizSessionRepository savedQuizSessionRepository;
	private final ObjectMapper objectMapper;

	public QuizDraftService(SavedQuizSessionRepository savedQuizSessionRepository, ObjectMapper objectMapper) {
		this.savedQuizSessionRepository = savedQuizSessionRepository;
		this.objectMapper = objectMapper;
	}

	public SavedQuizSession createDraft(Student student, List<Long> questionIds, int totalQuestions, int secondsRemaining, String mode, String difficultyBand, String topicSummary) {
		savedQuizSessionRepository.findFirstByStudentIdAndActiveTrueOrderByUpdatedAtDesc(student.getId())
				.ifPresent(existing -> {
					existing.setActive(false);
					savedQuizSessionRepository.save(existing);
				});

		SavedQuizSession draft = new SavedQuizSession(student, writeQuestionIds(questionIds), totalQuestions, secondsRemaining, mode, difficultyBand, topicSummary);
		return savedQuizSessionRepository.save(draft);
	}

	public Optional<SavedQuizSession> findActiveDraft(Long studentId) {
		if (studentId == null) {
			return Optional.empty();
		}
		return savedQuizSessionRepository.findFirstByStudentIdAndActiveTrueOrderByUpdatedAtDesc(studentId);
	}

	public Optional<SavedQuizSession> updateDraft(Long draftId, Long studentId, Map<String, String> answers, Integer secondsRemaining) {
		return savedQuizSessionRepository.findByIdAndStudentId(draftId, studentId)
				.filter(SavedQuizSession::isActive)
				.map(draft -> {
					if (answers != null) {
						draft.setAnswersJson(writeAnswers(answers));
					}
					if (secondsRemaining != null) {
						draft.setSecondsRemaining(secondsRemaining);
					}
					return savedQuizSessionRepository.save(draft);
				});
	}

	public void markCompleted(Long draftId, Long studentId) {
		if (draftId == null || studentId == null) {
			return;
		}
		savedQuizSessionRepository.findByIdAndStudentId(draftId, studentId)
				.ifPresent(draft -> {
					draft.setActive(false);
					savedQuizSessionRepository.save(draft);
				});
	}

	public List<Long> readQuestionIds(SavedQuizSession draft) {
		try {
			return objectMapper.readValue(draft.getQuestionIdsJson(), new TypeReference<>() {});
		} catch (JacksonException ex) {
			return List.of();
		}
	}

	public Map<String, String> readAnswers(SavedQuizSession draft) {
		try {
			return objectMapper.readValue(draft.getAnswersJson(), new TypeReference<>() {});
		} catch (JacksonException ex) {
			return Map.of();
		}
	}

	private String writeQuestionIds(List<Long> questionIds) {
		try {
			return objectMapper.writeValueAsString(questionIds == null ? List.of() : new ArrayList<>(questionIds));
		} catch (JacksonException ex) {
			return "[]";
		}
	}

	private String writeAnswers(Map<String, String> answers) {
		try {
			return objectMapper.writeValueAsString(answers == null ? Map.of() : new LinkedHashMap<>(answers));
		} catch (JacksonException ex) {
			return "{}";
		}
	}
}
