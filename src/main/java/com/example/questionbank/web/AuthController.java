package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.StudentRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
	private final AuthService authService;
	private final StudentRepository studentRepository;

	public AuthController(AuthService authService, StudentRepository studentRepository) {
		this.authService = authService;
		this.studentRepository = studentRepository;
	}

	@GetMapping("/login")
	public String loginPage(Model model, @RequestParam(value = "error", required = false) String error) {
		prepareAuthModel(model, "login", error);
		return "auth/auth";
	}

	@PostMapping("/login")
	public String login(@Valid @ModelAttribute("loginForm") LoginForm form, BindingResult bindingResult, Model model, HttpSession session) {
		if (bindingResult.hasErrors()) {
			prepareAuthModel(model, "login", null);
			return "auth/auth";
		}

		return authService.authenticate(form.getEmail(), form.getPassword())
				.map(student -> {
					session.setAttribute(SessionKeys.STUDENT_ID, student.getId());
					return "redirect:/dashboard";
				})
				.orElse("redirect:/login?error=Invalid+email+or+password");
	}

	@GetMapping("/register")
	public String registerPage(Model model) {
		prepareAuthModel(model, "register", null);
		return "auth/auth";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult bindingResult, Model model, HttpSession session) {
		if (form.getPassword() != null && form.getConfirmPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
		}

		if (form.getEmail() != null && studentRepository.findByEmailIgnoreCase(form.getEmail().trim()).isPresent()) {
			bindingResult.rejectValue("email", "email.taken", "Email already registered");
		}

		if (bindingResult.hasErrors()) {
			prepareAuthModel(model, "register", null);
			return "auth/auth";
		}

		var student = authService.register(form.getFullName().trim(), form.getEmail(), form.getPassword());
		session.setAttribute(SessionKeys.STUDENT_ID, student.getId());
		return "redirect:/dashboard";
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/";
	}

	private void prepareAuthModel(Model model, String activeMode, String error) {
		if (!model.containsAttribute("loginForm")) {
			model.addAttribute("loginForm", new LoginForm());
		}
		if (!model.containsAttribute("registerForm")) {
			model.addAttribute("registerForm", new RegisterForm());
		}
		model.addAttribute("activeMode", activeMode);
		model.addAttribute("error", error);
	}
}

