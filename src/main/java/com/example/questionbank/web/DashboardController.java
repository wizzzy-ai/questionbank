package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.QuizDraftService;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {
	private final AuthService authService;
	private final QuizResultRepository quizResultRepository;
	private final QuizDraftService quizDraftService;

	public DashboardController(AuthService authService, QuizResultRepository quizResultRepository, QuizDraftService quizDraftService) {
		this.authService = authService;
		this.quizResultRepository = quizResultRepository;
		this.quizDraftService = quizDraftService;
	}

	@GetMapping("/dashboard")
	public String dashboard(HttpSession session, Model model) {
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
		if (studentId == null) {
			session.invalidate();
			return "redirect:/login";
		}
		Student student = authService.findById(studentId).orElse(null);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}
		if (student.isAdmin()) {
			return "redirect:/admin";
		}
		model.addAttribute("student", student);
		List<com.example.questionbank.QuizResult> results = quizResultRepository.findTop20ByStudentIdOrderBySubmittedAtDesc(studentId);
		int currentStreak = 0;
		for (var result : results) {
			if (result.getPercentage() >= 60.0) {
				currentStreak++;
			} else {
				break;
			}
		}
		model.addAttribute("results", results);
		model.addAttribute("quizCount", quizResultRepository.countByStudentId(studentId));
		model.addAttribute("bestPct", quizResultRepository.bestPercentage(studentId));
		model.addAttribute("lastScorePct", results.isEmpty() ? 0.0 : results.get(0).getPercentage());
		model.addAttribute("currentStreak", currentStreak);
		model.addAttribute("totalPoints", student.getTotalPoints());
		model.addAttribute("leaderboardVisible", student.isLeaderboardVisible());
		model.addAttribute("activeDraft", quizDraftService.findActiveDraft(studentId).orElse(null));
		return "dashboard";
	}
}
