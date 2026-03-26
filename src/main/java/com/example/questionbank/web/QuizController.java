package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.BookmarkRepository;
import com.example.questionbank.Question;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
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

	@GetMapping("/quiz/start")
	public String start(
			@RequestParam(value = "count", defaultValue = "10") int count,
			@RequestParam(value = "difficulty", required = false) String difficulty,
			HttpSession session,
			Model model
	) {
		int quizCount = Math.max(5, Math.min(25, count));

		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);

		List<Question> questions;
		if (difficulty != null && !difficulty.isBlank()) {
			questions = pickQuestionsForDifficulty(quizCount, difficulty.trim().toUpperCase());
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

		Set<Long> bookmarkedQuestionIds = Set.of();
		if (studentId != null) {
			bookmarkedQuestionIds = new HashSet<>(bookmarkRepository.findBookmarkedQuestionIds(studentId, ids));
		}
		model.addAttribute("bookmarkedQuestionIds", bookmarkedQuestionIds);

		return "quiz";
	}

	private List<Question> pickQuestionsForDifficulty(int quizCount, String band) {
		int poolSize = Math.min(quizCount * 8, 200);
		List<Question> pool = questionRepository.findRandomQuestions(poolSize);

		return pool.stream()
				.filter(q -> q.getDifficultyBand().name().equalsIgnoreCase(band))
				.limit(quizCount)
				.collect(Collectors.toList());
	}

	private List<Question> pickAdaptiveQuestionsForStudent(int quizCount, Long studentId) {
		long totalAnswered = quizAnswerRepository.countByStudentId(studentId);
		long correctAnswered = quizAnswerRepository.countByStudentIdAndCorrectTrue(studentId);

		double accuracy = totalAnswered == 0 ? 0.65 : (correctAnswered * 1.0 / totalAnswered);
		String targetBand = accuracy >= 0.80 ? "HARD" : (accuracy >= 0.60 ? "MEDIUM" : "EASY");

		List<Question> picked = pickQuestionsForDifficulty(quizCount, targetBand);
		if (picked.size() >= quizCount) {
			return picked;
		}

		List<Long> pickedIds = picked.stream().map(Question::getId).filter(Objects::nonNull).toList();
		List<Question> fallbackPool = questionRepository.findRandomQuestions(Math.min(quizCount * 10, 250));
		for (Question q : fallbackPool) {
			if (picked.size() >= quizCount) {
				break;
			}
			if (q.getId() != null && pickedIds.contains(q.getId())) {
				continue;
			}
			picked.add(q);
		}
		return picked;
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
		return "result";
	}
}
