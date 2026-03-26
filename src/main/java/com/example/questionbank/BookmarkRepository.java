package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
	Optional<Bookmark> findByStudentIdAndQuestionId(Long studentId, Long questionId);

	long countByStudentId(Long studentId);

	@Query("""
			select b.question.id
			from Bookmark b
			where b.student.id = :studentId
			  and b.question.id in :questionIds
			""")
	List<Long> findBookmarkedQuestionIds(@Param("studentId") Long studentId, @Param("questionIds") Collection<Long> questionIds);

	@Query("""
			select b
			from Bookmark b
			join fetch b.question q
			where b.student.id = :studentId
			order by b.createdAt desc
			""")
	List<Bookmark> findAllByStudentIdFetchQuestionOrderByCreatedAtDesc(@Param("studentId") Long studentId);
}
