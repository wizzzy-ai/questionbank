package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.BookmarkRepository;
import com.example.questionbank.Question;
import com.example.questionbank.QuestionDifficultyBand;
import com.example.questionbank.QuestionRepository;
import com.example.questionbank.QuizAnswer;
import com.example.questionbank.QuizAnswerRepository;
import com.example.questionbank.QuizDraftService;
import com.example.questionbank.QuizResult;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.SavedQuizSession;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class QuizController {
	private final AuthService authService;
	private final QuestionRepository questionRepository;
	private final QuizResultRepository quizResultRepository;
	private final BookmarkRepository bookmarkRepository;
	private final QuizAnswerRepository quizAnswerRepository;
	private final QuizDraftService quizDraftService;

	public QuizController(
			AuthService authService,
			QuestionRepository questionRepository,
			QuizResultRepository quizResultRepository,
			BookmarkRepository bookmarkRepository,
			QuizAnswerRepository quizAnswerRepository,
			QuizDraftService quizDraftService
	) {
		this.authService = authService;
		this.questionRepository = questionRepository;
		this.quizResultRepository = quizResultRepository;
		this.bookmarkRepository = bookmarkRepository;
		this.quizAnswerRepository = quizAnswerRepository;
		this.quizDraftService = quizDraftService;
	}

	@GetMapping("/quiz/setup")
	public String setup(HttpSession session, Model model) {
		Student student = findStudent(session);
		if (student != null) {
			model.addAttribute("student", student);
			model.addAttribute("activeDraft", quizDraftService.findActiveDraft(student.getId()).orElse(null));
		}
		model.addAttribute("topics", questionRepository.findDistinctCategories());
		return "quiz_setup";
	}

	@GetMapping("/quiz/start")
	public String start(
			@RequestParam(value = "count", defaultValue = "10") int count,
			@RequestParam(value = "difficulty", required = false) String difficulty,
			@RequestParam(value = "topics", required = false) List<String> topics,
			@RequestParam(value = "mode", required = false) String mode,
			@RequestParam(value = "draftId", required = false) Long draftId,
			HttpSession session,
			Model model
	) {
		Student student = findStudent(session);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}

		if (draftId != null) {
			SavedQuizSession draft = quizDraftService.findActiveDraft(student.getId())
					.filter(saved -> saved.getId().equals(draftId))
					.orElse(null);
			if (draft != null) {
				return renderDraft(student, draft, session, model);
			}
		}

		int quizCount = Math.max(5, Math.min(25, count));
		List<String> selectedTopics = normalizeTopics(topics);
		String normalizedDifficulty = normalizeDifficulty(difficulty);
		String normalizedMode = mode == null ? "" : mode.trim().toLowerCase();

		List<Question> questions;
		String effectiveMode;
		if ("random".equals(normalizedMode)) {
			questions = pickQuestions(quizCount, null, List.of());
			effectiveMode = "RANDOM";
		} else if (!selectedTopics.isEmpty() || normalizedDifficulty != null) {
			questions = pickQuestions(quizCount, normalizedDifficulty, selectedTopics);
			effectiveMode = "CUSTOM";
		} else {
			questions = pickAdaptiveQuestionsForStudent(quizCount, student.getId());
			effectiveMode = "ADAPTIVE";
		}

		return renderQuiz(student, questions, session, model, effectiveMode, normalizedDifficulty, selectedTopics, null, Map.of(), quizCount * 60);
	}

	@GetMapping("/quiz/bookmarks/start")
	public String startFromBookmarks(HttpSession session, Model model) {
		Student student = findStudent(session);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}

		List<Question> questions = bookmarkRepository.findAllByStudentIdFetchQuestionOrderByCreatedAtDesc(student.getId()).stream()
				.map(bookmark -> bookmark.getQuestion())
				.distinct()
				.limit(25)
				.toList();

		return renderQuiz(student, questions, session, model, "BOOKMARKS", null, questions.stream().map(Question::getCategory).distinct().toList(), null, Map.of(), Math.max(questions.size(), 5) * 60);
	}

	@PostMapping("/quiz/draft")
	@ResponseBody
	public Map<String, Object> updateDraft(
			@RequestParam("draftId") Long draftId,
			@RequestParam(value = "secondsRemaining", required = false) Integer secondsRemaining,
			@RequestParam(value = "answersJson", required = false) String answersJson,
			HttpSession session
	) {
		Student student = findStudent(session);
		if (student == null) {
			return Map.of("saved", false);
		}

		Map<String, String> answers = new HashMap<>();
		if (answersJson != null && !answersJson.isBlank()) {
			for (String pair : answersJson.split(",")) {
				String[] parts = pair.split(":");
				if (parts.length == 2) {
					answers.put(parts[0], parts[1]);
				}
			}
		}

		boolean saved = quizDraftService.updateDraft(draftId, student.getId(), answers, secondsRemaining).isPresent();
		return Map.of("saved", saved);
	}

	@PostMapping("/quiz/submit")
	public String submit(HttpServletRequest request, HttpSession session, Model model) {
		@SuppressWarnings("unchecked")
		List<Long> ids = (List<Long>) session.getAttribute(SessionKeys.QUIZ_QUESTION_IDS);
		Integer total = (Integer) session.getAttribute(SessionKeys.QUIZ_TOTAL);
		if (ids == null || ids.isEmpty() || total == null) {
			return "redirect:/dashboard";
		}

		List<Question> questions = questionRepository.findAllById(ids).stream()
				.sorted(Comparator.comparing(q -> ids.indexOf(q.getId())))
				.toList();

		int score = 0;
		List<QuestionResultItem> breakdown = new ArrayList<>();
		for (Question q : questions) {
			String userAnswer = request.getParameter("q_" + q.getId());
			boolean correct = userAnswer != null
					&& userAnswer.trim().equalsIgnoreCase(q.getCorrectOption());
			if (correct) {
				score++;
			}
			breakdown.add(new QuestionResultItem(q, userAnswer, q.getCorrectOption(), correct));
		}

		Student student = findStudent(session);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}

		double percentage = total == 0 ? 0.0 : (score * 100.0 / total);
		String mode = (String) session.getAttribute(SessionKeys.QUIZ_MODE);
		String difficulty = (String) session.getAttribute(SessionKeys.QUIZ_DIFFICULTY);
		@SuppressWarnings("unchecked")
		List<String> topics = (List<String>) session.getAttribute(SessionKeys.QUIZ_TOPICS);
		Integer secondsRemaining = parseInteger(request.getParameter("secondsRemaining"));
		int durationSeconds = Math.max(0, total * 60 - (secondsRemaining == null ? 0 : secondsRemaining));

		QuizResult result = quizResultRepository.save(new QuizResult(
				student,
				score,
				total,
				percentage,
				mode,
				difficulty,
				summarizeTopics(topics),
				durationSeconds
		));

		int pointsAwarded = calculatePoints(score, total, percentage);
		student.setTotalPoints(student.getTotalPoints() + pointsAwarded);
		authService.save(student);

		List<QuizAnswer> answersToSave = new ArrayList<>(questions.size());
		for (QuestionResultItem item : breakdown) {
			Question q = item.question();
			answersToSave.add(new QuizAnswer(
					student,
					result,
					q,
					item.userAnswer(),
					item.correctAnswer(),
					item.correct(),
					q.getCategory(),
					result.getSubmittedAt()
			));
		}
		quizAnswerRepository.saveAll(answersToSave);

		for (QuestionResultItem item : breakdown) {
			item.question().recordAttempt(item.correct());
		}
		questionRepository.saveAll(questions);

		Long draftId = (Long) session.getAttribute(SessionKeys.ACTIVE_DRAFT_ID);
		quizDraftService.markCompleted(draftId, student.getId());
		clearQuizSession(session);

		model.addAttribute("result", result);
		model.addAttribute("student", student);
		model.addAttribute("breakdown", breakdown);
		model.addAttribute("pointsAwarded", pointsAwarded);
		model.addAttribute("totalPoints", student.getTotalPoints());
		return "result";
	}

	private String renderDraft(Student student, SavedQuizSession draft, HttpSession session, Model model) {
		List<Long> ids = quizDraftService.readQuestionIds(draft);
		List<Question> questions = questionRepository.findAllById(ids).stream()
				.sorted(Comparator.comparing(q -> ids.indexOf(q.getId())))
				.toList();
		Map<String, String> answers = quizDraftService.readAnswers(draft);
		List<String> topics = draft.getTopicSummary() == null || draft.getTopicSummary().isBlank()
				? List.of()
				: List.of(draft.getTopicSummary().split(", "));

		return renderQuiz(student, questions, session, model, draft.getMode(), draft.getDifficultyBand(), topics, draft, answers, draft.getSecondsRemaining());
	}

	private String renderQuiz(
			Student student,
			List<Question> questions,
			HttpSession session,
			Model model,
			String mode,
			String difficulty,
			List<String> selectedTopics,
			SavedQuizSession existingDraft,
			Map<String, String> savedAnswers,
			int secondsRemaining
	) {
		if (questions.isEmpty()) {
			model.addAttribute("student", student);
			model.addAttribute("message", "No questions found for that quiz yet.");
			return "quiz_empty";
		}

		List<Long> ids = questions.stream().map(Question::getId).filter(Objects::nonNull).toList();
		session.setAttribute(SessionKeys.QUIZ_QUESTION_IDS, new ArrayList<>(ids));
		session.setAttribute(SessionKeys.QUIZ_TOTAL, questions.size());
		session.setAttribute(SessionKeys.QUIZ_MODE, mode);
		session.setAttribute(SessionKeys.QUIZ_DIFFICULTY, difficulty);
		session.setAttribute(SessionKeys.QUIZ_TOPICS, selectedTopics == null ? List.of() : new ArrayList<>(selectedTopics));
		session.setAttribute(SessionKeys.QUIZ_STARTED_AT, Instant.now().toEpochMilli());

		SavedQuizSession draft = existingDraft != null
				? existingDraft
				: quizDraftService.createDraft(student, ids, questions.size(), secondsRemaining, mode, difficulty, summarizeTopics(selectedTopics));
		session.setAttribute(SessionKeys.ACTIVE_DRAFT_ID, draft.getId());

		model.addAttribute("student", student);
		model.addAttribute("questions", questions.stream().map(QuizQuestionView::from).toList());
		model.addAttribute("total", questions.size());
		model.addAttribute("selectedTopics", selectedTopics);
		model.addAttribute("selectedDifficulty", difficulty);
		model.addAttribute("quizMode", mode);
		model.addAttribute("timeLimitSeconds", secondsRemaining);
		model.addAttribute("activeDraftId", draft.getId());
		model.addAttribute("savedAnswers", savedAnswers == null ? Map.of() : savedAnswers);

		Set<Long> bookmarkedQuestionIds = new HashSet<>(bookmarkRepository.findBookmarkedQuestionIds(student.getId(), ids));
		model.addAttribute("bookmarkedQuestionIds", bookmarkedQuestionIds);

		return "quiz";
	}

	private int calculatePoints(int score, int total, double percentage) {
		int completionPoints = 10;
		int correctAnswerPoints = score * 5;
		int perfectBonus = score == total && total > 0 ? 25 : 0;
		int performanceBonus = percentage >= 90.0 ? 20 : (percentage >= 75.0 ? 12 : (percentage >= 60.0 ? 6 : 0));
		return completionPoints + correctAnswerPoints + perfectBonus + performanceBonus;
	}

	private List<Question> pickQuestions(int quizCount, String band, List<String> topics) {
		List<Question> pool = fetchPool(band, topics);
		Collections.shuffle(pool);
		if (pool.size() >= quizCount) {
			return new ArrayList<>(pool.subList(0, quizCount));
		}

		List<Question> fallbackPool = fetchFallbackPool(quizCount, topics);
		Set<Long> pickedIds = pool.stream().map(Question::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		for (Question question : fallbackPool) {
			if (pool.size() >= quizCount) {
				break;
			}
			if (question.getId() != null && pickedIds.contains(question.getId())) {
				continue;
			}
			pool.add(question);
			if (question.getId() != null) {
				pickedIds.add(question.getId());
			}
		}
		return pool;
	}

	private List<Question> pickAdaptiveQuestionsForStudent(int quizCount, Long studentId) {
		long totalAnswered = quizAnswerRepository.countByStudentId(studentId);
		long correctAnswered = quizAnswerRepository.countByStudentIdAndCorrectTrue(studentId);

		double accuracy = totalAnswered == 0 ? 0.65 : (correctAnswered * 1.0 / totalAnswered);
		String targetBand = accuracy >= 0.80 ? "HARD" : (accuracy >= 0.60 ? "MEDIUM" : "EASY");
		return pickQuestions(quizCount, targetBand, List.of());
	}

	private List<Question> fetchPool(String band, Collection<String> topics) {
		QuestionDifficultyBand difficultyBand = parseDifficultyBand(band);
		if (difficultyBand != null && topics != null && !topics.isEmpty()) {
			return new ArrayList<>(questionRepository.findByDifficultyBandAndCategoryIn(difficultyBand, topics));
		}
		if (difficultyBand != null) {
			return new ArrayList<>(questionRepository.findByDifficultyBand(difficultyBand));
		}
		if (topics != null && !topics.isEmpty()) {
			return new ArrayList<>(questionRepository.findByCategoryIn(topics));
		}
		return new ArrayList<>(questionRepository.findRandomQuestions(250));
	}

	private List<Question> fetchFallbackPool(int quizCount, Collection<String> topics) {
		if (topics != null && !topics.isEmpty()) {
			return new ArrayList<>(questionRepository.findByCategoryIn(topics));
		}
		return new ArrayList<>(questionRepository.findRandomQuestions(Math.min(quizCount * 10, 250)));
	}

	private QuestionDifficultyBand parseDifficultyBand(String band) {
		if (band == null || band.isBlank()) {
			return null;
		}
		try {
			return QuestionDifficultyBand.valueOf(band.trim().toUpperCase());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private String normalizeDifficulty(String difficulty) {
		QuestionDifficultyBand band = parseDifficultyBand(difficulty);
		return band == null ? null : band.name();
	}

	private List<String> normalizeTopics(List<String> topics) {
		if (topics == null || topics.isEmpty()) {
			return List.of();
		}
		return topics.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(topic -> !topic.isBlank())
				.distinct()
				.toList();
	}

	private Student findStudent(HttpSession session) {
		Object studentIdAttr = session.getAttribute(SessionKeys.STUDENT_ID);
		Long studentId = null;
		if (studentIdAttr instanceof Long id) {
			studentId = id;
		} else if (studentIdAttr instanceof Number number) {
			studentId = number.longValue();
		} else if (studentIdAttr instanceof String text) {
			try {
				studentId = Long.parseLong(text);
			} catch (NumberFormatException ignored) {
				studentId = null;
			}
		}
		return authService.findById(studentId).orElse(null);
	}

	private String summarizeTopics(List<String> topics) {
		if (topics == null || topics.isEmpty()) {
			return "Mixed topics";
		}
		return String.join(", ", topics);
	}

	private Integer parseInteger(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private void clearQuizSession(HttpSession session) {
		session.removeAttribute(SessionKeys.QUIZ_QUESTION_IDS);
		session.removeAttribute(SessionKeys.QUIZ_TOTAL);
		session.removeAttribute(SessionKeys.QUIZ_MODE);
		session.removeAttribute(SessionKeys.QUIZ_DIFFICULTY);
		session.removeAttribute(SessionKeys.QUIZ_TOPICS);
		session.removeAttribute(SessionKeys.QUIZ_STARTED_AT);
		session.removeAttribute(SessionKeys.ACTIVE_DRAFT_ID);
	}
}
