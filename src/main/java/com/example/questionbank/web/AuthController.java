package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.EmailService;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import com.example.questionbank.StudentRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthController {
	private final AuthService authService;
	private final StudentRepository studentRepository;
	private final EmailService emailService;

	public AuthController(AuthService authService, StudentRepository studentRepository, EmailService emailService) {
		this.authService = authService;
		this.studentRepository = studentRepository;
		this.emailService = emailService;
	}

	@GetMapping("/login")
	public String loginPage(
			Model model,
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "info", required = false) String info
	) {
		prepareAuthModel(model, "login", error, info);
		return "auth/auth";
	}

	@PostMapping("/login")
	public String login(@Valid @ModelAttribute("loginForm") LoginForm form, BindingResult bindingResult, Model model, HttpSession session) {
		if (bindingResult.hasErrors()) {
			prepareAuthModel(model, "login", null, null);
			return "auth/auth";
		}

		return authService.authenticate(form.getEmail(), form.getPassword())
				.map(student -> {
					if (!student.isEmailVerified()) {
						return "redirect:/verify-email/notice?email=" + student.getEmail();
					}
					session.setAttribute(SessionKeys.STUDENT_ID, student.getId());
					return student.isAdmin() ? "redirect:/admin" : "redirect:/dashboard";
				})
				.orElse("redirect:/login?error=Invalid+credentials+or+account+temporarily+locked");
	}

	@GetMapping("/register")
	public String registerPage(Model model, @RequestParam(value = "info", required = false) String info) {
		prepareAuthModel(model, "register", null, info);
		return "auth/auth";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult bindingResult, Model model) {
		if (form.getPassword() != null && form.getConfirmPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
		}

		if (form.getEmail() != null && studentRepository.findByEmailIgnoreCase(form.getEmail().trim()).isPresent()) {
			bindingResult.rejectValue("email", "email.taken", "Email already registered");
		}

		if (bindingResult.hasErrors()) {
			prepareAuthModel(model, "register", null, null);
			return "auth/auth";
		}

		Student student = authService.register(form.getFullName().trim(), form.getEmail(), form.getPassword());
		return "redirect:/verify-email/notice?email=" + student.getEmail();
	}

	@GetMapping("/verify-email/notice")
	public String verifyEmailNotice(@RequestParam("email") String email, Model model) {
		model.addAttribute("email", email);
		model.addAttribute("remainingAttempts", authService.getRemainingResendAttempts(email));
		return "auth/verify_notice";
	}

	@PostMapping("/verify-email")
	public String verifyEmailWithOtp(@RequestParam("email") String email,
	                                  @RequestParam("otp") String otp,
	                                  Model model,
	                                  HttpSession session) {
		var verifiedStudent = authService.verifyEmailWithOtp(email, otp);
		if (verifiedStudent.isPresent()) {
			// Create session immediately after successful verification
			session.setAttribute(SessionKeys.STUDENT_ID, verifiedStudent.get().getId());
			return verifiedStudent.get().isAdmin() ? "redirect:/admin" : "redirect:/dashboard";
		} else {
			model.addAttribute("email", email);
			model.addAttribute("error", "Invalid or expired verification code. Please try again.");
			return "auth/verify_notice";
		}
	}

	@PostMapping("/verify-email/resend")
	public String resendVerification(@RequestParam("email") String email, Model model) {
		// Check rate limiting
		if (!authService.canResendCode(email)) {
			model.addAttribute("email", email);
			model.addAttribute("error", "Too many resend attempts. Please try again in 1 hour.");
			return "auth/verify_notice";
		}
		
		// Record this attempt
		authService.recordResendAttempt(email);
		
		// Generate and send new code
		authService.issueVerificationTokenForEmail(email);
		
		model.addAttribute("email", email);
		model.addAttribute("info", "A new verification code has been sent to your email.");
		model.addAttribute("remainingAttempts", authService.getRemainingResendAttempts(email));
		model.addAttribute("justResent", true);
		return "auth/verify_notice";
	}

	@GetMapping("/forgot-password")
	public String forgotPasswordPage(Model model, @RequestParam(value = "info", required = false) String info) {
		if (!model.containsAttribute("passwordResetRequestForm")) {
			model.addAttribute("passwordResetRequestForm", new PasswordResetRequestForm());
		}
		model.addAttribute("info", info);
		return "auth/forgot_password";
	}

	@PostMapping("/forgot-password")
	public String forgotPassword(@Valid @ModelAttribute("passwordResetRequestForm") PasswordResetRequestForm form, BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			return "auth/forgot_password";
		}
		Student student = authService.issuePasswordResetToken(form.getEmail()).orElse(null);
		model.addAttribute("email", form.getEmail());
		model.addAttribute("resetToken", student != null ? student.getPasswordResetToken() : null);
		return "auth/reset_notice";
	}

	@GetMapping("/reset-password")
	public String resetPasswordPage(@RequestParam("token") String token, Model model) {
		PasswordResetForm form = new PasswordResetForm();
		form.setToken(token);
		model.addAttribute("passwordResetForm", form);
		return "auth/reset_password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(@Valid @ModelAttribute("passwordResetForm") PasswordResetForm form, BindingResult bindingResult) {
		if (form.getPassword() != null && form.getConfirmPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
		}
		if (bindingResult.hasErrors()) {
			return "auth/reset_password";
		}
		boolean reset = authService.resetPassword(form.getToken(), form.getPassword()).isPresent();
		return reset
				? "redirect:/login?info=Password+updated.+Please+sign+in."
				: "redirect:/login?error=That+reset+link+is+invalid+or+expired";
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/";
	}

	@GetMapping("/test-email")
	@ResponseBody
	public String testEmail() {
		try {
			System.out.println("=== TESTING EMAIL SEND ===");
			String testEmail = "samjosh013@gmail.com";
			String testCode = "123456";
			
			System.out.println("Sending test email to: " + testEmail);
			System.out.println("Test OTP: " + testCode);
			
			emailService.sendVerificationEmail(testEmail, "Test User", testCode);
			
			return "✅ Test email sent successfully to " + testEmail + ". Check your inbox!";
		} catch (Exception e) {
			System.out.println("❌ Test email failed: " + e.getMessage());
			e.printStackTrace();
			return "❌ Test email failed: " + e.getMessage();
		}
	}

	private void prepareAuthModel(Model model, String activeMode, String error, String info) {
		if (!model.containsAttribute("loginForm")) {
			model.addAttribute("loginForm", new LoginForm());
		}
		if (!model.containsAttribute("registerForm")) {
			model.addAttribute("registerForm", new RegisterForm());
		}
		model.addAttribute("activeMode", activeMode);
		model.addAttribute("error", error);
		model.addAttribute("info", info);
	}
}
