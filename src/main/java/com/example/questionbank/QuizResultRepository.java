package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
	List<QuizResult> findTop8ByOrderBySubmittedAtDesc();

	List<QuizResult> findTop20ByStudentIdOrderBySubmittedAtDesc(Long studentId);

	List<QuizResult> findTop5ByStudentIdOrderByPercentageDescSubmittedAtAsc(Long studentId);

	List<QuizResult> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

	Optional<QuizResult> findByIdAndStudentId(Long id, Long studentId);

	long countByStudentId(Long studentId);

	@Query("select coalesce(avg(r.percentage), 0.0) from QuizResult r where r.student.id = :studentId")
	double averagePercentage(@Param("studentId") Long studentId);

	@Query("select coalesce(max(r.percentage), 0.0) from QuizResult r where r.student.id = :studentId")
	double bestPercentage(@Param("studentId") Long studentId);

	@Query("""
			select count(distinct r.student.id)
			from QuizResult r
			where r.submittedAt >= :start
			""")
	long countDistinctStudentsActiveSince(@Param("start") java.time.Instant start);

	@Query("""
			select count(r)
			from QuizResult r
			where r.submittedAt >= :start
			  and r.submittedAt < :end
			""")
	long countSubmittedBetween(@Param("start") Instant start, @Param("end") Instant end);

	void deleteByStudentId(Long studentId);

	@Query("""
			select count(distinct r.student.id)
			from QuizResult r
			where r.submittedAt >= :start
			  and r.submittedAt < :end
			""")
	long countDistinctStudentsSubmittedBetween(@Param("start") Instant start, @Param("end") Instant end);

	@Query("""
			select function('date', r.submittedAt), count(r)
			from QuizResult r
			where r.submittedAt >= :start
			  and r.submittedAt < :end
			group by function('date', r.submittedAt)
			order by function('date', r.submittedAt)
			""")
	List<Object[]> countSubmittedGroupedByDate(@Param("start") Instant start, @Param("end") Instant end);
}
