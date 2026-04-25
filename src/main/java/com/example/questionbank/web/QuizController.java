package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.BookmarkRepository;
import com.example.questionbank.Question;
import com.example.questionbank.QuestionDifficultyBand;
import com.example.questionbank.QuestionRepository;
import com.example.questionbank.QuizAnswer;
import com.example.questionbank.QuizAnswerRepository;
import com.example.questionbank.QuizResult;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Set;

@Controller
public class QuizController {
	private final AuthService authService;
	private final QuestionRepository questionRepository;
	private final QuizResultRepository quizResultRepository;
	private final BookmarkRepository bookmarkRepository;
	private final QuizAnswerRepository quizAnswerRepository;

	public QuizController(AuthService authService, QuestionRepository questionRepository, QuizResultRepository quizResultRepository, BookmarkRepository bookmarkRepository, QuizAnswerRepository quizAnswerRepository) {
		this.authService = authService;
		this.questionRepository = questionRepository;
		this.quizResultRepository = quizResultRepository;
		this.bookmarkRepository = bookmarkRepository;
		this.quizAnswerRepository = quizAnswerRepository;
	}

	@GetMapping("/quiz/setup")
	public String setup(HttpSession session, Model model) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		authService.findById(studentId).ifPresent(student -> model.addAttribute("student", student));
		model.addAttribute("topics", questionRepository.findDistinctCategories());
		return "quiz_setup";
	}

	@GetMapping("/quiz/start")
	public String start(
			@RequestParam(value = "count", defaultValue = "10") int count,
			@RequestParam(value = "difficulty", required = false) String difficulty,
			@RequestParam(value = "topics", required = false) List<String> topics,
			@RequestParam(value = "mode", required = false) String mode,
			HttpSession session,
			Model model
	) {
		int quizCount = Math.max(5, Math.min(25, count));

		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		List<String> selectedTopics = normalizeTopics(topics);
		String normalizedDifficulty = normalizeDifficulty(difficulty);
		String normalizedMode = mode == null ? "" : mode.trim().toLowerCase();

		List<Question> questions;
		if ("random".equals(normalizedMode)) {
			questions = pickQuestions(quizCount, null, List.of());
		} else if (!selectedTopics.isEmpty() || normalizedDifficulty != null) {
			questions = pickQuestions(quizCount, normalizedDifficulty, selectedTopics);
		} else if (studentId != null) {
			questions = pickAdaptiveQuestionsForStudent(quizCount, studentId);
		} else {
			questions = questionRepository.findRandomQuestions(quizCount);
		}
		if (questions.isEmpty()) {
			model.addAttribute("message", "No questions found in the database yet.");
			return "quiz_empty";
		}

		List<Long> ids = questions.stream().map(Question::getId).filter(Objects::nonNull).toList();
		session.setAttribute(SessionKeys.QUIZ_QUESTION_IDS, new ArrayList<>(ids));
		session.setAttribute(SessionKeys.QUIZ_TOTAL, questions.size());

		authService.findById(studentId).ifPresent(s -> model.addAttribute("student", s));

			List<QuizQuestionView> viewQuestions = questions.stream()
					.map(QuizQuestionView::from)
					.toList();
			model.addAttribute("questions", viewQuestions);
			model.addAttribute("total", questions.size());
			model.addAttribute("selectedTopics", selectedTopics);
			model.addAttribute("selectedDifficulty", normalizedDifficulty);

		Set<Long> bookmarkedQuestionIds = Set.of();
		if (studentId != null) {
			bookmarkedQuestionIds = new HashSet<>(bookmarkRepository.findBookmarkedQuestionIds(studentId, ids));
		}
		model.addAttribute("bookmarkedQuestionIds", bookmarkedQuestionIds);

		return "quiz";
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
			if (correct) score++;
			breakdown.add(new QuestionResultItem(q, userAnswer, q.getCorrectOption(), correct));
		}

		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		Student student = authService.findById(studentId).orElse(null);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}

		double percentage = total == 0 ? 0.0 : (score * 100.0 / total);
		QuizResult result = quizResultRepository.save(new QuizResult(student, score, total, percentage));
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

			session.removeAttribute(SessionKeys.QUIZ_QUESTION_IDS);
			session.removeAttribute(SessionKeys.QUIZ_TOTAL);

		model.addAttribute("result", result);
		model.addAttribute("student", student);
		model.addAttribute("breakdown", breakdown);
		model.addAttribute("pointsAwarded", pointsAwarded);
		model.addAttribute("totalPoints", student.getTotalPoints());
		return "result";
	}

	private int calculatePoints(int score, int total, double percentage) {
		int completionPoints = 10;
		int correctAnswerPoints = score * 5;
		int perfectBonus = score == total && total > 0 ? 25 : 0;
		int performanceBonus = percentage >= 90.0 ? 20 : (percentage >= 75.0 ? 12 : (percentage >= 60.0 ? 6 : 0));
		return completionPoints + correctAnswerPoints + perfectBonus + performanceBonus;
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
}
