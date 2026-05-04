package com.example.questionbank;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.example.questionbank.XssSanitizer;

@Service
public class AuthService {
	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
	private static final int MAX_VERIFICATION_ATTEMPTS = 5;
	private static final int MAX_RESEND_PER_HOUR = 3;
	private static final Duration VERIFICATION_CODE_EXPIRY = Duration.ofMinutes(10);

	private final StudentRepository studentRepository;
	private final XssSanitizer xssSanitizer;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final SecureRandom secureRandom = new SecureRandom();

	// Rate limiting: email -> list of resend timestamps
	private final Map<String, java.util.List<Instant>> resendAttempts = new ConcurrentHashMap<>();

	public AuthService(StudentRepository studentRepository, XssSanitizer xssSanitizer, EmailService emailService) {
		this.studentRepository = studentRepository;
		this.xssSanitizer = xssSanitizer;
		this.emailService = emailService;
	}

	public Optional<Student> findById(Long id) {
		return studentRepository.findById(id);
	}

	public Optional<Student> authenticate(String email, String password) {
		if (email == null || password == null) {
			return Optional.empty();
		}
		return studentRepository.findByEmailIgnoreCaseAndDeletedFalse(email.trim())
				.flatMap(student -> {
					if (student.isBanned() || student.isDeleted()) {
						return Optional.empty();
					}
					if (student.getLockUntil() != null && student.getLockUntil().isAfter(Instant.now())) {
						return Optional.empty();
					}
					if (!passwordEncoder.matches(password, student.getPasswordHash())) {
						recordFailedLogin(student);
						return Optional.empty();
					}
					resetFailedLoginState(student);
					// Only generate OTP if email is not verified and this is NOT a login attempt
					// Login attempts should not generate OTP - that's handled by registration flow
					if (!student.isEmailVerified()) {
						logger.info("🔓 User {} is not verified, login denied", student.getEmail());
						return Optional.empty();
					} else {
						logger.info("✅ User {} is verified, proceeding with login", student.getEmail());
					}
					return Optional.of(studentRepository.save(student));
				});
	}

	public Student register(String fullName, String email, String rawPassword) {
		logger.info("=== USER REGISTRATION ===");
		logger.info("Registering user: {} ({})", fullName, email);
		
		String sanitizedName = xssSanitizer.sanitize(fullName == null ? null : fullName.trim());
		String normalizedEmail = email == null ? null : email.trim().toLowerCase();
		String hash = passwordEncoder.encode(rawPassword);
		Student student = new Student(sanitizedName, normalizedEmail, hash);
		student.setRole(StudentRole.USER);
		student.setEmailVerified(false);
		student.setBanned(false);
		if (student.getCreatedAt() == null) {
			student.setCreatedAt(Instant.now());
		}
		
		// Check rate limiting before generating OTP
		if (!canResendCode(normalizedEmail)) {
			logger.warn("🚫 RATE LIMIT EXCEEDED - Too many OTP requests for: {}", normalizedEmail);
			logger.warn("Remaining attempts: {}", getRemainingResendAttempts(normalizedEmail));
			throw new RuntimeException("Too many OTP requests. Please try again later.");
		}
		
		logger.info("🔧 Generating OTP for user: {}", normalizedEmail);
		issueVerificationToken(student);
		logger.info("✅ OTP generated for user: {}", normalizedEmail);
		
		studentRepository.save(student);
		logger.info("✅ Student saved to database");
		
		logger.info("🚀 Sending verification email to: {}", student.getEmail());
		emailService.sendVerificationEmail(student.getEmail(), student.getFullName(), student.getVerificationToken());
		logger.info("✅ Verification email send initiated");
		
		// Record this OTP request for rate limiting
		recordResendAttempt(normalizedEmail);
		
		return student;
	}

	public Optional<Student> updateFullName(Long studentId, String fullName) {
		if (studentId == null || fullName == null || fullName.isBlank()) {
			return Optional.empty();
		}
		String sanitizedName = xssSanitizer.sanitize(fullName.trim());
		return studentRepository.findById(studentId)
				.map(student -> {
					student.setFullName(sanitizedName);
					return studentRepository.save(student);
				});
	}

	public Optional<Student> updateProfile(Long studentId, String fullName, boolean leaderboardVisible) {
		if (studentId == null || fullName == null || fullName.isBlank()) {
			return Optional.empty();
		}
		String sanitizedName = xssSanitizer.sanitize(fullName.trim());
		return studentRepository.findById(studentId)
				.map(student -> {
					student.setFullName(sanitizedName);
					student.setLeaderboardVisible(leaderboardVisible);
					return studentRepository.save(student);
				});
	}

	public Student save(Student student) {
		return studentRepository.save(student);
	}

	public Optional<Student> verifyEmail(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		return studentRepository.findByVerificationToken(token)
				.filter(student -> student.getVerificationTokenExpiresAt() != null
						&& student.getVerificationTokenExpiresAt().isAfter(Instant.now()))
				.map(student -> {
					student.setEmailVerified(true);
					student.setVerificationToken(null);
					student.setVerificationTokenExpiresAt(null);
					return studentRepository.save(student);
				});
	}

	public Optional<Student> issueVerificationTokenForEmail(String email) {
		logger.info("=== RESEND VERIFICATION TOKEN ===");
		logger.info("Requested for email: {}", email);
		
		if (email == null || email.isBlank()) {
			logger.warn("❌ Email is null or blank");
			return Optional.empty();
		}
		return studentRepository.findByEmailIgnoreCaseAndDeletedFalse(email.trim())
				.map(student -> {
					logger.info("🔧 Generating new OTP for user: {}", student.getEmail());
					issueVerificationToken(student);
					studentRepository.save(student);
					logger.info("✅ New OTP generated for user: {}", student.getEmail());
					
					logger.info("🚀 Sending verification email to: {}", student.getEmail());
					emailService.sendVerificationEmail(student.getEmail(), student.getFullName(), student.getVerificationToken());
					logger.info("✅ Verification email send initiated");
					
					return student;
				});
	}

	public Optional<Student> issuePasswordResetToken(String email) {
		if (email == null || email.isBlank()) {
			return Optional.empty();
		}
		return studentRepository.findByEmailIgnoreCaseAndDeletedFalse(email.trim())
				.map(student -> {
					student.setPasswordResetToken(UUID.randomUUID().toString().replace("-", ""));
					student.setPasswordResetTokenExpiresAt(Instant.now().plus(Duration.ofHours(1)));
					studentRepository.save(student);
					emailService.sendPasswordResetEmail(student.getEmail(), student.getFullName(), student.getPasswordResetToken());
					return student;
				});
	}

	public Optional<Student> resetPassword(String token, String rawPassword) {
		if (token == null || token.isBlank() || rawPassword == null || rawPassword.isBlank()) {
			return Optional.empty();
		}
		return studentRepository.findByPasswordResetToken(token)
				.filter(student -> student.getPasswordResetTokenExpiresAt() != null
						&& student.getPasswordResetTokenExpiresAt().isAfter(Instant.now()))
				.map(student -> {
					student.setPasswordHash(passwordEncoder.encode(rawPassword));
					student.setPasswordResetToken(null);
					student.setPasswordResetTokenExpiresAt(null);
					resetFailedLoginState(student);
					return studentRepository.save(student);
				});
	}

	private void recordFailedLogin(Student student) {
		int attempts = student.getFailedLoginAttempts() + 1;
		student.setFailedLoginAttempts(attempts);
		if (attempts >= 5) {
			student.setLockUntil(Instant.now().plus(Duration.ofMinutes(15)));
			student.setFailedLoginAttempts(0);
		}
		studentRepository.save(student);
	}

	private void resetFailedLoginState(Student student) {
		student.setFailedLoginAttempts(0);
		student.setLockUntil(null);
	}

	private Student issueVerificationToken(Student student) {
		logger.info("=== GENERATING VERIFICATION TOKEN ===");
		logger.info("Student: {} ({})", student.getFullName(), student.getEmail());
		
		// Generate 6-digit OTP using SecureRandom
		int code = 100000 + secureRandom.nextInt(900000);
		String otpCode = String.valueOf(code);
		
		student.setVerificationToken(otpCode);
		student.setVerificationTokenExpiresAt(Instant.now().plus(VERIFICATION_CODE_EXPIRY));
		student.setVerificationAttempts(0);
		
		logger.info("✅ OTP generated for user: {}", student.getEmail());
		logger.info("✅ OTP expires at: {}", student.getVerificationTokenExpiresAt());
		
		return student;
	}

	public Optional<Student> verifyEmailWithOtp(String email, String otp) {
		if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
			return Optional.empty();
		}
		String normalizedEmail = email.trim().toLowerCase();
		return studentRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
				.filter(student -> {
					// Check if too many attempts
					if (student.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
						return false;
					}
					// Check code match and expiry
					boolean valid = student.getVerificationToken() != null
							&& student.getVerificationToken().equals(otp)
							&& student.getVerificationTokenExpiresAt() != null
							&& student.getVerificationTokenExpiresAt().isAfter(Instant.now());
					if (!valid) {
						// Increment attempts on failure
						student.setVerificationAttempts(student.getVerificationAttempts() + 1);
						studentRepository.save(student);
					}
					return valid;
				})
				.map(student -> {
					// Success - clear verification data
					student.setEmailVerified(true);
					student.setVerificationToken(null);
					student.setVerificationTokenExpiresAt(null);
					student.setVerificationAttempts(0);
					return studentRepository.save(student);
				});
	}

	public boolean canResendCode(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		String normalizedEmail = email.trim().toLowerCase();
		java.util.List<Instant> attempts = resendAttempts.getOrDefault(normalizedEmail, new java.util.ArrayList<>());
		
		// Remove attempts older than 1 hour
		Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
		attempts.removeIf(timestamp -> timestamp.isBefore(oneHourAgo));
		
		return attempts.size() < MAX_RESEND_PER_HOUR;
	}

	public void recordResendAttempt(String email) {
		if (email == null || email.isBlank()) {
			return;
		}
		String normalizedEmail = email.trim().toLowerCase();
		resendAttempts.computeIfAbsent(normalizedEmail, k -> new java.util.ArrayList<>()).add(Instant.now());
	}

	public int getRemainingResendAttempts(String email) {
		if (email == null || email.isBlank()) {
			return 0;
		}
		String normalizedEmail = email.trim().toLowerCase();
		java.util.List<Instant> attempts = resendAttempts.getOrDefault(normalizedEmail, new java.util.ArrayList<>());
		
		// Remove attempts older than 1 hour
		Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
		attempts.removeIf(timestamp -> timestamp.isBefore(oneHourAgo));
		
		return Math.max(0, MAX_RESEND_PER_HOUR - attempts.size());
	}
	
	public StudentRepository getStudentRepository() {
		return studentRepository;
	}
}
