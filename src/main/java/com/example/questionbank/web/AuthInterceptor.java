package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
	private final AuthService authService;

	public AuthInterceptor(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String uri = request.getRequestURI();
		if (uri.equals("/") || uri.equals("/favicon.svg")) {
			return true;
		}
		if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") || uri.startsWith("/webjars/")) {
			return true;
		}
		if (uri.equals("/login")
				|| uri.equals("/register")
				|| uri.equals("/forgot-password")
				|| uri.equals("/reset-password")
				|| uri.equals("/verify-email/confirm")
				|| uri.equals("/verify-email/notice")
				|| uri.startsWith("/verify-email")  // Allow POST /verify-email and /verify-email/resend
				|| uri.equals("/terms")
				|| uri.equals("/privacy")
				|| uri.equals("/error")) {
			return true;
		}
		if (uri.startsWith("/api/")) {
			return true;
		}

		HttpSession session = request.getSession(false);
		Long studentId = session == null ? null : readStudentId(session);
		if (studentId == null) {
			response.sendRedirect("/login");
			return false;
		}

		Student student = authService.findById(studentId).orElse(null);
		if (student == null) {
			if (session != null) {
				session.invalidate();
			}
			response.sendRedirect("/login");
			return false;
		}

		if (student.isBanned()) {
			if (session != null) {
				session.invalidate();
			}
			response.sendRedirect("/login?error=Account+access+disabled");
			return false;
		}

		if (uri.startsWith("/admin") && !student.isAdmin()) {
			response.sendRedirect("/dashboard");
			return false;
		}

		return true;
	}

	private Long readStudentId(HttpSession session) {
		Object studentIdAttr = session.getAttribute(SessionKeys.STUDENT_ID);
		if (studentIdAttr instanceof Long studentId) {
			return studentId;
		}
		if (studentIdAttr instanceof Number number) {
			return number.longValue();
		}
		if (studentIdAttr instanceof String text) {
			try {
				return Long.parseLong(text);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
