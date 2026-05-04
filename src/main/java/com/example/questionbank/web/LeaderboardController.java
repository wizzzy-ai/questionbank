package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.QuizAnswerRepository;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import com.example.questionbank.StudentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;

@Controller
public class LeaderboardController {
	private final AuthService authService;
	private final StudentRepository studentRepository;
	private final QuizResultRepository quizResultRepository;
	private final QuizAnswerRepository quizAnswerRepository;

	public LeaderboardController(
			AuthService authService,
			StudentRepository studentRepository,
			QuizResultRepository quizResultRepository,
			QuizAnswerRepository quizAnswerRepository
	) {
		this.authService = authService;
		this.studentRepository = studentRepository;
		this.quizResultRepository = quizResultRepository;
		this.quizAnswerRepository = quizAnswerRepository;
	}

	@GetMapping("/leaderboard")
	public String leaderboard(HttpSession session, Model model) {
		Student viewer = getViewer(session);
		if (viewer != null) {
			model.addAttribute("student", viewer);
		}

		model.addAttribute("entries", studentRepository.findTop25ByLeaderboardVisibleTrueAndAdminFalseAndDeletedFalseOrderByTotalPointsDescIdAsc());
		model.addAttribute("viewerId", viewer != null ? viewer.getId() : null);
		return "leaderboard";
	}

	@GetMapping("/players/{id}")
	public String playerProfile(@PathVariable("id") Long id, HttpSession session, Model model) {
		Student viewer = getViewer(session);
		if (viewer != null) {
			model.addAttribute("student", viewer);
		}

		Student target = studentRepository.findById(id).orElse(null);
		if (target == null) {
			return "redirect:/leaderboard";
		}
		if (target.isAdmin() || target.isDeleted()) {
			return "redirect:/leaderboard";
		}

		boolean isOwner = viewer != null && viewer.getId() != null && viewer.getId().equals(target.getId());
		if (!target.isLeaderboardVisible() && !isOwner) {
			model.addAttribute("player", target);
			model.addAttribute("privateProfile", true);
			return "player_profile";
		}

		long totalAnswered = quizAnswerRepository.countByStudentId(target.getId());
		long correctAnswered = quizAnswerRepository.countByStudentIdAndCorrectTrue(target.getId());
		double accuracyPct = totalAnswered == 0 ? 0.0 : (correctAnswered * 100.0 / totalAnswered);
		var categoryRows = quizAnswerRepository.categoryAccuracy(target.getId());
		var strongestCategory = categoryRows.stream()
				.max(Comparator.comparingDouble(row -> row.getTotal() == null || row.getTotal() == 0
						? 0.0
						: (row.getCorrect() == null ? 0.0 : (row.getCorrect() * 100.0 / row.getTotal()))))
				.orElse(null);

		model.addAttribute("player", target);
		model.addAttribute("privateProfile", false);
		model.addAttribute("quizCount", quizResultRepository.countByStudentId(target.getId()));
		model.addAttribute("avgPct", quizResultRepository.averagePercentage(target.getId()));
		model.addAttribute("bestPct", quizResultRepository.bestPercentage(target.getId()));
		model.addAttribute("totalAnswered", totalAnswered);
		model.addAttribute("accuracyPct", accuracyPct);
		model.addAttribute("strongestCategory", strongestCategory);
		model.addAttribute("recentResults", quizResultRepository.findTop5ByStudentIdOrderByPercentageDescSubmittedAtAsc(target.getId()));
		model.addAttribute("leaderboardRank", target.isLeaderboardVisible() && !target.isAdmin()
				? studentRepository.countLeaderboardEntriesAhead(target.getTotalPoints(), target.getId()) + 1
				: null);

		return "player_profile";
	}

	private Student getViewer(HttpSession session) {
		Object studentIdAttr = session.getAttribute(SessionKeys.STUDENT_ID);
		if (!(studentIdAttr instanceof Long studentId)) {
			return null;
		}
		return authService.findById(studentId).orElse(null);
	}
}
