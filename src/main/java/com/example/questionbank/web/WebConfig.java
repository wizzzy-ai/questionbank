package com.example.questionbank.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	private final AuthInterceptor authInterceptor;
	private final RateLimitingInterceptor rateLimitingInterceptor;

	public WebConfig(AuthInterceptor authInterceptor, RateLimitingInterceptor rateLimitingInterceptor) {
		this.authInterceptor = authInterceptor;
		this.rateLimitingInterceptor = rateLimitingInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// Rate limiting for auth endpoints
		registry.addInterceptor(rateLimitingInterceptor)
				.addPathPatterns("/login", "/register", "/forgot-password", "/verify-email/resend");

		// Auth check for all endpoints
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/**");
	}
}

