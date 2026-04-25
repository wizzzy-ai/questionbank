package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.BookmarkRepository;
import com.example.questionbank.QuizAnswerRepository;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import com.example.questionbank.StudentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Controller
public class ProfileController {
	private final AuthService authService;
	private final QuizResultRepository quizResultRepository;
	private final QuizAnswerRepository quizAnswerRepository;
	private final BookmarkRepository bookmarkRepository;
	private final StudentRepository studentRepository;

	public ProfileController(
			AuthService authService,
			QuizResultRepository quizResultRepository,
			QuizAnswerRepository quizAnswerRepository,
			BookmarkRepository bookmarkRepository,
			StudentRepository studentRepository
	) {
		this.authService = authService;
		this.quizResultRepository = quizResultRepository;
		this.quizAnswerRepository = quizAnswerRepository;
		this.bookmarkRepository = bookmarkRepository;
		this.studentRepository = studentRepository;
	}

	@GetMapping("/profile")
	public String profile(
			@RequestParam(value = "updated", required = false) Boolean updated,
			HttpSession session,
			Model model
	) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		Student student = authService.findById(studentId).orElse(null);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}

		long totalAnswered = quizAnswerRepository.countByStudentId(studentId);
		long correctAnswered = quizAnswerRepository.countByStudentIdAndCorrectTrue(studentId);
		double accuracyPct = totalAnswered == 0 ? 0.0 : (correctAnswered * 100.0 / totalAnswered);

		var recentResults = quizResultRepository.findTop20ByStudentIdOrderBySubmittedAtDesc(studentId);
		List<String> recentLabels = new ArrayList<>(recentResults.size());
		List<Double> recentPercentages = new ArrayList<>(recentResults.size());
		DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd MMM").withZone(ZoneId.systemDefault());
		for (var r : recentResults) {
			recentLabels.add(labelFmt.format(r.getSubmittedAt()));
			recentPercentages.add(r.getPercentage());
		}
		Collections.reverse(recentLabels);
		Collections.reverse(recentPercentages);

		var categoryRows = quizAnswerRepository.categoryAccuracy(studentId);
		List<String> categoryLabels = new ArrayList<>(categoryRows.size());
		List<Double> categoryPercentages = new ArrayList<>(categoryRows.size());
		for (var row : categoryRows) {
			long total = row.getTotal() == null ? 0 : row.getTotal();
			long correct = row.getCorrect() == null ? 0 : row.getCorrect();
			double pct = total == 0 ? 0.0 : (correct * 100.0 / total);
			categoryLabels.add(row.getCategory());
			categoryPercentages.add(pct);
		}
		var strongestCategory = categoryRows.stream()
				.max(Comparator.comparingDouble(row -> row.getTotal() == null || row.getTotal() == 0
						? 0.0
						: (row.getCorrect() == null ? 0.0 : (row.getCorrect() * 100.0 / row.getTotal()))))
				.orElse(null);

		model.addAttribute("student", student);
		model.addAttribute("quizCount", quizResultRepository.countByStudentId(studentId));
		model.addAttribute("avgPct", quizResultRepository.averagePercentage(studentId));
		model.addAttribute("bestPct", quizResultRepository.bestPercentage(studentId));
		model.addAttribute("bookmarksCount", bookmarkRepository.countByStudentId(studentId));
		model.addAttribute("totalAnswered", totalAnswered);
		model.addAttribute("correctAnswered", correctAnswered);
		model.addAttribute("accuracyPct", accuracyPct);
		model.addAttribute("categoryAccuracy", categoryRows);
		model.addAttribute("recentLabels", recentLabels);
		model.addAttribute("recentPercentages", recentPercentages);
		model.addAttribute("categoryLabels", categoryLabels);
		model.addAttribute("categoryPercentages", categoryPercentages);
		model.addAttribute("topResults", quizResultRepository.findTop5ByStudentIdOrderByPercentageDescSubmittedAtAsc(studentId));
		model.addAttribute("strongestCategory", strongestCategory);
		model.addAttribute("totalPoints", student.getTotalPoints());
		model.addAttribute("leaderboardVisible", student.isLeaderboardVisible());
		model.addAttribute("leaderboardRank", student.isLeaderboardVisible()
				? studentRepository.countLeaderboardEntriesAhead(student.getTotalPoints(), student.getId()) + 1
				: null);
		model.addAttribute("updated", updated != null && updated);

		return "profile";
	}

	@PostMapping("/profile/update")
	public String updateProfile(
			@RequestParam("fullName") String fullName,
			@RequestParam(value = "leaderboardVisible", defaultValue = "false") boolean leaderboardVisible,
			HttpSession session
	) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		var updated = authService.updateProfile(studentId, fullName, leaderboardVisible);
		if (updated.isEmpty()) {
			return "redirect:/profile";
		}
		return "redirect:/profile?updated=true";
	}
}
