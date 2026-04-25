package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
	List<QuizResult> findTop20ByStudentIdOrderBySubmittedAtDesc(Long studentId);

	List<QuizResult> findTop5ByStudentIdOrderByPercentageDescSubmittedAtAsc(Long studentId);

	long countByStudentId(Long studentId);

	@Query("select coalesce(avg(r.percentage), 0.0) from QuizResult r where r.student.id = :studentId")
	double averagePercentage(@Param("studentId") Long studentId);

	@Query("select coalesce(max(r.percentage), 0.0) from QuizResult r where r.student.id = :studentId")
	double bestPercentage(@Param("studentId") Long studentId);
}
