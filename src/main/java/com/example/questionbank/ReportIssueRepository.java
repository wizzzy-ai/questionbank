package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReportIssueRepository extends JpaRepository<ReportIssue, Long> {

	long countByStatus(ReportIssue.Status status);

	@Query("""
			select count(r)
			from ReportIssue r
			where r.status = :status
			  and r.createdAt >= :start
			  and r.createdAt < :end
			""")
	long countByStatusBetween(@Param("status") ReportIssue.Status status, @Param("start") Instant start, @Param("end") Instant end);

	List<ReportIssue> findByStatusOrderByCreatedAtDesc(ReportIssue.Status status);
}
