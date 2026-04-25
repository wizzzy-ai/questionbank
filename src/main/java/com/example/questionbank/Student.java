package com.example.questionbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

	public Student() {
	}

	public Student(String fullName, String email, String passwordHash) {
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.totalPoints = 0;
		this.leaderboardVisible = true;
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
}

