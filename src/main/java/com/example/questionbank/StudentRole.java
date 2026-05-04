package com.example.questionbank;

public enum StudentRole {
	USER,
	ADMIN,
	SUPER_ADMIN;

	public boolean hasAdminAccess() {
		return this == ADMIN || this == SUPER_ADMIN;
	}
}
