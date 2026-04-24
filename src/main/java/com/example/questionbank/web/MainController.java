package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
	private final AuthService authService;

	public MainController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/")
	public String home(HttpSession session) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		if (studentId == null) {
			return "welcome";
		}
		Student student = authService.findById(studentId).orElse(null);
		if (student == null) {
			session.invalidate();
			return "welcome";
		}
		return "redirect:/dashboard";
	}
}

