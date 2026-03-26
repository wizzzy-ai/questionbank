package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
	private final AuthService authService;
	private final QuizResultRepository quizResultRepository;

	public DashboardController(AuthService authService, QuizResultRepository quizResultRepository) {
		this.authService = authService;
		this.quizResultRepository = quizResultRepository;
	}

	@GetMapping("/dashboard")
	public String dashboard(HttpSession session, Model model) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		Student student = authService.findById(studentId).orElse(null);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}
		model.addAttribute("student", student);
		model.addAttribute("results", quizResultRepository.findTop20ByStudentIdOrderBySubmittedAtDesc(studentId));
		return "dashboard";
	}
}
