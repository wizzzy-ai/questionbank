package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
	Optional<Student> findByEmailIgnoreCase(String email);

	Optional<Student> findByEmailIgnoreCaseAndDeletedFalse(String email);

	Optional<Student> findByVerificationToken(String verificationToken);

	Optional<Student> findByPasswordResetToken(String passwordResetToken);

	Optional<Student> findByRememberMeToken(String rememberMeToken);

	List<Student> findAllByRole(StudentRole role);

	Optional<Student> findFirstByRoleOrderByCreatedAtAscIdAsc(StudentRole role);

	List<Student> findTop8ByDeletedFalseOrderByCreatedAtDescIdDesc();

	List<Student> findTop25ByLeaderboardVisibleTrueAndAdminFalseAndDeletedFalseOrderByTotalPointsDescIdAsc();

	Optional<Student> findByIdAndLeaderboardVisibleTrueAndAdminFalseAndDeletedFalse(Long id);

	@Query("""
			select count(s)
			from Student s
			where s.leaderboardVisible = true
			  and s.admin = false
			  and s.deleted = false
			  and (s.totalPoints > :points or (s.totalPoints = :points and s.id < :studentId))
			""")
	long countLeaderboardEntriesAhead(@Param("points") int points, @Param("studentId") Long studentId);

	long countByAdminTrue();

	long countByRole(StudentRole role);

	long countByBannedFalseAndDeletedFalse();

	@Query("""
			select count(s)
			from Student s
			where s.createdAt >= :start
			  and s.createdAt < :end
			""")
	long countAllUsersBetween(@Param("start") Instant start, @Param("end") Instant end);

	@Query("""
			select count(s)
			from Student s
			where s.admin = false
			  and s.deleted = false
			  and s.createdAt >= :start
			  and s.createdAt < :end
			""")
	long countStudentsBetween(@Param("start") Instant start, @Param("end") Instant end);

	@Deprecated
	@Query("""
			select count(s)
			from Student s
			where s.admin = false
			  and s.deleted = false
			  and s.createdAt >= :start
			  and s.createdAt < :end
			""")
	long countRegisteredBetween(@Param("start") Instant start, @Param("end") Instant end);

	@Query("""
			select function('date', s.createdAt), count(s)
			from Student s
			where s.admin = false
			  and s.deleted = false
			  and s.createdAt >= :start
			  and s.createdAt < :end
			group by function('date', s.createdAt)
			order by function('date', s.createdAt)
			""")
	List<Object[]> countRegisteredGroupedByDate(@Param("start") Instant start, @Param("end") Instant end);

	@Query("""
			select count(s)
			from Student s
			where s.deleted = false
			  and s.lastLoginAt >= :start
			  and s.lastLoginAt < :end
			""")
	long countActiveBetween(@Param("start") Instant start, @Param("end") Instant end);
}

