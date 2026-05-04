package com.example.questionbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "students")
public class Student {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String fullName;

	@Column(nullable = false, unique = true, length = 160)
	private String email;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false)
	private int totalPoints = 0;

	@Column(nullable = false)
	private boolean leaderboardVisible = true;

	@Column(nullable = false)
	private boolean admin = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private StudentRole role = StudentRole.USER;

	@Column(name = "bootstrap_admin", nullable = false)
	private boolean bootstrapAdmin = false;

	@Column(nullable = false)
	private boolean emailVerified = false;

	@Column(nullable = false)
	private boolean banned = false;

	@Column(nullable = false)
	private boolean deleted = false;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(length = 120)
	private String verificationToken;

	private Instant verificationTokenExpiresAt;

	@Column(nullable = false)
	private int verificationAttempts = 0;

	@Column(length = 120)
	private String passwordResetToken;

	private Instant passwordResetTokenExpiresAt;

	@Column(nullable = false)
	private int failedLoginAttempts = 0;

	private Instant lockUntil;

	public Student() {
	}

	public Student(String fullName, String email, String passwordHash) {
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.totalPoints = 0;
		this.leaderboardVisible = true;
		setRole(StudentRole.USER);
		this.emailVerified = true;
		this.banned = false;
		this.deleted = false;
		this.deletedAt = null;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public int getTotalPoints() {
		return totalPoints;
	}

	public void setTotalPoints(int totalPoints) {
		this.totalPoints = Math.max(0, totalPoints);
	}

	public boolean isLeaderboardVisible() {
		return leaderboardVisible;
	}

	public void setLeaderboardVisible(boolean leaderboardVisible) {
		this.leaderboardVisible = leaderboardVisible;
	}

	public boolean isAdmin() {
		return admin || (role != null && role.hasAdminAccess());
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
		if (admin && (role == null || role == StudentRole.USER)) {
			this.role = StudentRole.ADMIN;
		}
		if (!admin && role != StudentRole.SUPER_ADMIN) {
			this.role = StudentRole.USER;
		}
	}

	public StudentRole getRole() {
		if (role == null) {
			return admin ? StudentRole.ADMIN : StudentRole.USER;
		}
		if (role == StudentRole.USER && admin) {
			return StudentRole.ADMIN;
		}
		return role;
	}

	public void setRole(StudentRole role) {
		this.role = role == null ? StudentRole.USER : role;
		this.admin = this.role.hasAdminAccess();
	}

	public boolean isSuperAdmin() {
		return getRole() == StudentRole.SUPER_ADMIN;
	}

	public boolean isBootstrapAdmin() {
		return bootstrapAdmin;
	}

	public void setBootstrapAdmin(boolean bootstrapAdmin) {
		this.bootstrapAdmin = bootstrapAdmin;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public boolean isBanned() {
		return banned;
	}

	public void setBanned(boolean banned) {
		this.banned = banned;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
		if (!deleted) {
			this.deletedAt = null;
		}
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Instant lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	@PrePersist
	void ensureCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public String getVerificationToken() {
		return verificationToken;
	}

	public void setVerificationToken(String verificationToken) {
		this.verificationToken = verificationToken;
	}

	public Instant getVerificationTokenExpiresAt() {
		return verificationTokenExpiresAt;
	}

	public void setVerificationTokenExpiresAt(Instant verificationTokenExpiresAt) {
		this.verificationTokenExpiresAt = verificationTokenExpiresAt;
	}

	public int getVerificationAttempts() {
		return verificationAttempts;
	}

	public void setVerificationAttempts(int verificationAttempts) {
		this.verificationAttempts = verificationAttempts;
	}

	public String getPasswordResetToken() {
		return passwordResetToken;
	}

	public void setPasswordResetToken(String passwordResetToken) {
		this.passwordResetToken = passwordResetToken;
	}

	public Instant getPasswordResetTokenExpiresAt() {
		return passwordResetTokenExpiresAt;
	}

	public void setPasswordResetTokenExpiresAt(Instant passwordResetTokenExpiresAt) {
		this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public void setFailedLoginAttempts(int failedLoginAttempts) {
		this.failedLoginAttempts = Math.max(0, failedLoginAttempts);
	}

	public Instant getLockUntil() {
		return lockUntil;
	}

	public void setLockUntil(Instant lockUntil) {
		this.lockUntil = lockUntil;
	}
}

