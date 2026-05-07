package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavedQuizSessionRepository extends JpaRepository<SavedQuizSession, Long> {
	Optional<SavedQuizSession> findFirstByStudentIdAndActiveTrueOrderByUpdatedAtDesc(Long studentId);

	Optional<SavedQuizSession> findByIdAndStudentId(Long id, Long studentId);
}
