package com.example.questionbank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap {
	private final StudentRepository studentRepository;
	private final AdminRoleService adminRoleService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Value("${app.bootstrap-admin.enabled:true}")
	private boolean enabled;

	@Value("${app.bootstrap-admin.full-name:${DEFAULT_ADMIN_FULL_NAME:Default Admin}}")
	private String adminFullName;

	@Value("${app.bootstrap-admin.email:${DEFAULT_ADMIN_EMAIL:admin@questionbank.local}}")
	private String adminEmail;

	@Value("${app.bootstrap-admin.password:${DEFAULT_ADMIN_PASSWORD:Admin@12345}}")
	private String adminPassword;

	public AdminBootstrap(StudentRepository studentRepository, AdminRoleService adminRoleService) {
		this.studentRepository = studentRepository;
		this.adminRoleService = adminRoleService;
	}

	@Order(5)
	@EventListener(ApplicationReadyEvent.class)
	public void ensureDefaultAdminIfMissing() {
		String normalizedEmail = adminEmail == null ? null : adminEmail.trim().toLowerCase();
		adminRoleService.reconcileRoles(normalizedEmail);

		if (!enabled || normalizedEmail == null || normalizedEmail.isBlank()) {
			return;
		}

		if (studentRepository.countByRole(StudentRole.SUPER_ADMIN) > 0) {
			studentRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(student -> {
				if (student.isSuperAdmin()) {
					student.setBootstrapAdmin(true);
					studentRepository.save(student);
				}
			});
			return;
		}

		Student existing = studentRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
		if (existing != null) {
			existing.setRole(StudentRole.SUPER_ADMIN);
			existing.setBootstrapAdmin(true);
			existing.setEmailVerified(true);
			studentRepository.save(existing);
			return;
		}

		Student admin = new Student(
				adminFullName == null || adminFullName.isBlank() ? "Default Admin" : adminFullName.trim(),
				normalizedEmail,
				passwordEncoder.encode(adminPassword == null || adminPassword.isBlank() ? "Admin@12345" : adminPassword)
		);
		admin.setRole(StudentRole.SUPER_ADMIN);
		admin.setBootstrapAdmin(true);
		admin.setEmailVerified(true);
		admin.setVerificationToken(null);
		admin.setVerificationTokenExpiresAt(null);
		admin.setPasswordResetToken(null);
		admin.setPasswordResetTokenExpiresAt(null);
		admin.setFailedLoginAttempts(0);
		admin.setLockUntil(null);
		studentRepository.save(admin);
	}
}
