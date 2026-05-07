package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
	@Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT :limit", nativeQuery = true)
	List<Question> findRandomQuestions(@Param("limit") int limit);

	List<Question> findByDifficultyBand(QuestionDifficultyBand difficultyBand);

	@Query("""
			select q
			from Question q
			where q.categoryEntity.name in :categories
			""")
	List<Question> findByCategoryIn(Collection<String> categories);

	@Query("""
			select q
			from Question q
			where q.difficultyBand = :difficultyBand
			  and q.categoryEntity.name in :categories
			""")
	List<Question> findByDifficultyBandAndCategoryIn(QuestionDifficultyBand difficultyBand, Collection<String> categories);

	List<Question> findByCategoryEntityId(Long categoryId);

	List<Question> findTop5ByCategoryEntityIdOrderByCreatedAtDescIdDesc(Long categoryId);

	long countByCategoryEntityId(Long categoryId);

	List<Question> findTop500ByCategoryEntityIsNull();

	long countByStatus(QuestionStatus status);

	List<Question> findTop500ByFingerprintIsNull();

	@Query("""
			select q.fingerprint
			from Question q
			where q.fingerprint is not null
			""")
	List<String> findAllFingerprints();

	@Query("""
			select distinct q.categoryEntity.name
			from Question q
			where q.categoryEntity is not null
			  and q.categoryEntity.name is not null
			  and trim(q.categoryEntity.name) <> ''
			order by q.categoryEntity.name asc
			""")
	List<String> findDistinctCategories();

	@Query("""
			select q
			from Question q
			where lower(q.questionText) like lower(concat('%', :query, '%'))
			   or lower(q.categoryEntity.name) like lower(concat('%', :query, '%'))
			order by q.categoryEntity.name asc, q.id desc
			""")
	List<Question> search(@Param("query") String query);

	@Query("""
			select q
			from Question q
			where lower(q.questionText) = lower(:questionText)
			  and lower(q.categoryEntity.name) = lower(:category)
			""")
	Optional<Question> findFirstByQuestionTextIgnoreCaseAndCategoryIgnoreCase(String questionText, String category);

	List<Question> findTop8ByOrderByCreatedAtDescIdDesc();

	@Query("""
			select count(q)
			from Question q
			where q.createdAt >= :start
			  and q.createdAt < :end
			""")
	long countCreatedBetween(@Param("start") Instant start, @Param("end") Instant end);

	@Query("""
			select q.categoryEntity.id, q.categoryEntity.name, count(q)
			from Question q
			where q.categoryEntity is not null
			  and q.createdAt >= :start
			  and q.createdAt < :end
			group by q.categoryEntity.id, q.categoryEntity.name
			order by count(q) desc, q.categoryEntity.name asc
			""")
	List<Object[]> countCreatedByCategoryBetween(@Param("start") Instant start, @Param("end") Instant end);

}
