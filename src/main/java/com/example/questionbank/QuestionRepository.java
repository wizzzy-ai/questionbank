package com.example.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
	@Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT :limit", nativeQuery = true)
	List<Question> findRandomQuestions(@Param("limit") int limit);

	List<Question> findByDifficultyBand(QuestionDifficultyBand difficultyBand);

	List<Question> findByCategoryIn(Collection<String> categories);

	List<Question> findByDifficultyBandAndCategoryIn(QuestionDifficultyBand difficultyBand, Collection<String> categories);

	List<Question> findTop500ByFingerprintIsNull();

	@Query("""
			select q.fingerprint
			from Question q
			where q.fingerprint is not null
			""")
	List<String> findAllFingerprints();

	@Query("""
			select distinct q.category
			from Question q
			where q.category is not null
			  and trim(q.category) <> ''
			order by q.category asc
			""")
	List<String> findDistinctCategories();

}
