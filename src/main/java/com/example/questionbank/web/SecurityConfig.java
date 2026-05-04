package com.example.questionbank.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.csrf(Customizer.withDefaults())
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> csp.policyDirectives(
								"default-src 'self'; " +
								"style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
								"script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
								"font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
								"img-src 'self' data:; " +
								"connect-src 'self' https://cdn.jsdelivr.net; " +
								"frame-ancestors 'none'"))
						.referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
						.frameOptions(frame -> frame.deny()))
				.sessionManagement(session -> session
						.sessionFixation(sessionFixation -> sessionFixation.migrateSession()))
				.httpBasic(Customizer.withDefaults())
				.formLogin(form -> form.disable())
				.logout(logout -> logout.disable());

		return http.build();
	}
}
