package com.example.questionbank.web;

import com.example.questionbank.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String uri = request.getRequestURI();
		if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") || uri.startsWith("/webjars/")) {
			return true;
		}
		if (uri.equals("/login") || uri.equals("/register") || uri.equals("/error")) {
			return true;
		}
		if (uri.startsWith("/api/")) {
			return true;
		}

		HttpSession session = request.getSession(false);
		Long studentId = session == null ? null : (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		if (studentId != null) {
			return true;
		}
		response.sendRedirect("/login");
		return false;
	}
}

