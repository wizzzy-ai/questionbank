package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.Question;
import com.example.questionbank.QuizAnswer;
import com.example.questionbank.QuizAnswerRepository;
import com.example.questionbank.QuizResult;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Controller
public class QuizHistoryController {
	private final AuthService authService;
	private final QuizResultRepository quizResultRepository;
	private final QuizAnswerRepository quizAnswerRepository;

	public QuizHistoryController(AuthService authService, QuizResultRepository quizResultRepository, QuizAnswerRepository quizAnswerRepository) {
		this.authService = authService;
		this.quizResultRepository = quizResultRepository;
		this.quizAnswerRepository = quizAnswerRepository;
	}

	@GetMapping("/results")
	public String results(HttpSession session, Model model) {
		Student student = getStudent(session);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}

		model.addAttribute("student", student);
		model.addAttribute("results", quizResultRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()));
		return "results";
	}

	@GetMapping("/results/{id}")
	public String resultDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
		Student student = getStudent(session);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}

		QuizResult result = quizResultRepository.findByIdAndStudentId(id, student.getId()).orElse(null);
		if (result == null) {
			return "redirect:/results";
		}

		List<QuestionResultItem> breakdown = new ArrayList<>();
		for (QuizAnswer answer : quizAnswerRepository.findByQuizResultIdOrderByIdAsc(result.getId())) {
			Question question = answer.getQuestion();
			breakdown.add(new QuestionResultItem(question, answer.getUserAnswer(), answer.getCorrectAnswer(), answer.isCorrect()));
		}

		model.addAttribute("student", student);
		model.addAttribute("result", result);
		model.addAttribute("breakdown", breakdown);
		model.addAttribute("pointsAwarded", result.getScore() * 5);
		model.addAttribute("totalPoints", student.getTotalPoints());
		return "result_detail";
	}

	private Student getStudent(HttpSession session) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		return authService.findById(studentId).orElse(null);
	}
}
