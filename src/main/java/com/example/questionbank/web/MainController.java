package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

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

	@GetMapping("/terms")
	public String terms(Model model) {
		model.addAttribute("pageType", "terms");
		model.addAttribute("pageTitle", "Terms of Service");
		model.addAttribute("summary", null);

		List<Map<String, String>> sections = List.of(
			Map.of(
				"title", "Acceptance of Terms",
				"content", "<p>By accessing or using Question Bank (\"the Service\"), you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the Service.</p>"
			),
			Map.of(
				"title", "Description of Service",
				"content", "<p>Question Bank is an online platform for Java programming practice, offering:</p><ul><li>Access to a curated bank of programming questions</li><li>Quiz and practice sessions with instant feedback</li><li>Progress tracking and analytics</li><li>Leaderboard and competitive features</li><li>Bookmarking and review functionality</li></ul>"
			),
			Map.of(
				"title", "User Accounts",
				"content", "<p>To use certain features of the Service, you must register for an account. You agree to:</p><ul><li>Provide accurate and complete information during registration</li><li>Maintain the security of your account credentials</li><li>Accept responsibility for all activities under your account</li><li>Notify us immediately of any unauthorized use</li></ul>"
			),
			Map.of(
				"title", "Acceptable Use",
				"content", "<p>You agree not to use the Service for any purpose that is unlawful or prohibited by these Terms. Prohibited activities include:</p><ul><li>Attempting to access other users' accounts</li><li>Sharing answers or circumventing the learning process</li><li>Using automated scripts or bots</li><li>Uploading malicious content or viruses</li><li>Harassing or disrupting other users</li></ul>"
			),
			Map.of(
				"title", "Intellectual Property",
				"content", "<p>All content on the Service, including questions, explanations, and code examples, is the property of Question Bank or its licensors. You may not reproduce, distribute, or create derivative works without permission.</p>"
			),
			Map.of(
				"title", "Termination",
				"content", "<p>We reserve the right to suspend or terminate your account at any time for violations of these Terms. You may also delete your account at any time through your profile settings.</p>"
			),
			Map.of(
				"title", "Disclaimer of Warranties",
				"content", "<p>The Service is provided \"as is\" without warranties of any kind. We do not guarantee that the Service will be error-free or uninterrupted.</p>"
			),
			Map.of(
				"title", "Limitation of Liability",
				"content", "<p>To the maximum extent permitted by law, Question Bank shall not be liable for any indirect, incidental, or consequential damages arising from your use of the Service.</p>"
			),
			Map.of(
				"title", "Changes to Terms",
				"content", "<p>We may modify these Terms at any time. Continued use of the Service after changes constitutes acceptance of the new Terms.</p>"
			),
			Map.of(
				"title", "Contact",
				"content", "<p>For questions about these Terms, please contact us at support@questionbank.local</p>"
			)
		);
		model.addAttribute("sections", sections);
		return "legal";
	}

	@GetMapping("/privacy")
	public String privacy(Model model) {
		model.addAttribute("pageType", "privacy");
		model.addAttribute("pageTitle", "Privacy Policy");
		model.addAttribute("summary", "<p><strong>Summary:</strong> We collect minimal data needed to provide our service. We don't sell your data to third parties. You can request deletion of your account and data at any time.</p>");

		List<Map<String, String>> sections = List.of(
			Map.of(
				"title", "Information We Collect",
				"content", "<p>Account information, quiz data, progress data, and technical data necessary to provide and improve our Service.</p>"
			),
			Map.of(
				"title", "How We Use Your Information",
				"content", "<p>Provide and maintain the Service, track progress, improve features, display leaderboards, send notifications, and ensure security.</p>"
			),
			Map.of(
				"title", "Data Storage and Security",
				"content", "<p>We implement industry-standard security measures including encryption, regular backups, and access controls to protect your data.</p>"
			),
			Map.of(
				"title", "Data Sharing",
				"content", "<p>We do not sell, trade, or rent your personal information. We may share data only with trusted service providers as needed to operate our platform.</p>"
			),
			Map.of(
				"title", "Your Rights",
				"content", "<p>Access, correct, delete your data, request data export, and opt out of leaderboards.</p>"
			),
			Map.of(
				"title", "Cookies and Tracking",
				"content", "<p>We use essential cookies for session management, security, and remembering your preferences. We do not use third-party tracking cookies.</p>"
			),
			Map.of(
				"title", "Data Retention",
				"content", "<p>We retain your data as long as your account is active. If you delete your account, personal identifiers are removed within 30 days.</p>"
			),
			Map.of(
				"title", "Children's Privacy",
				"content", "<p>The Service is not intended for children under 13. We do not knowingly collect data from children under 13.</p>"
			),
			Map.of(
				"title", "Changes to This Policy",
				"content", "<p>We may update this Privacy Policy periodically. We will notify you of significant changes via email or through the Service.</p>"
			),
			Map.of(
				"title", "Contact Us",
				"content", "<p>For privacy-related questions or to exercise your rights, contact us at:<br>Email: privacy@questionbank.local<br>Address: Question Bank Privacy Team</p>"
			)
		);
		model.addAttribute("sections", sections);
		return "legal";
	}
}
