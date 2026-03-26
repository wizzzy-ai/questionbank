package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
	long countByStudentId(Long studentId);

	long countByStudentIdAndCorrectTrue(Long studentId);

	@Query("""
			select a.category as category,
			       count(a) as total,
			       sum(case when a.correct = true then 1 else 0 end) as correct
			from QuizAnswer a
			where a.student.id = :studentId
			group by a.category
			order by count(a) desc
			""")
	List<CategoryAccuracyRow> categoryAccuracy(@Param("studentId") Long studentId);

	interface CategoryAccuracyRow {
		String getCategory();

		Long getTotal();

		Long getCorrect();
	}
}

