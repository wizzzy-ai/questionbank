package com.example.questionbank;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
	private final StudentRepository studentRepository;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AuthService(StudentRepository studentRepository) {
		this.studentRepository = studentRepository;
	}

	public Optional<Student> findById(Long id) {
		return studentRepository.findById(id);
	}

	public Optional<Student> authenticate(String email, String password) {
		if (email == null || password == null) {
			return Optional.empty();
		}
		return studentRepository.findByEmailIgnoreCase(email.trim())
				.filter(student -> passwordEncoder.matches(password, student.getPasswordHash()));
	}

	public Student register(String fullName, String email, String rawPassword) {
		String normalizedEmail = email == null ? null : email.trim().toLowerCase();
		String hash = passwordEncoder.encode(rawPassword);
		return studentRepository.save(new Student(fullName, normalizedEmail, hash));
	}

	public Optional<Student> updateFullName(Long studentId, String fullName) {
		if (studentId == null || fullName == null || fullName.isBlank()) {
			return Optional.empty();
		}
		return studentRepository.findById(studentId)
				.map(student -> {
					student.setFullName(fullName.trim());
					return studentRepository.save(student);
				});
	}
}
