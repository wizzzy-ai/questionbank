package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
	Optional<Student> findByEmailIgnoreCase(String email);

	List<Student> findTop25ByLeaderboardVisibleTrueOrderByTotalPointsDescIdAsc();

	Optional<Student> findByIdAndLeaderboardVisibleTrue(Long id);

	@Query("""
			select count(s)
			from Student s
			where s.leaderboardVisible = true
			  and (s.totalPoints > :points or (s.totalPoints = :points and s.id < :studentId))
			""")
	long countLeaderboardEntriesAhead(@Param("points") int points, @Param("studentId") Long studentId);
}

