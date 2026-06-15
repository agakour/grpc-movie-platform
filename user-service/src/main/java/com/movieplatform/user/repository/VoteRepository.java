package com.movieplatform.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.movieplatform.user.entity.MovieVote;
import com.movieplatform.user.entity.MovieVoteId;

public interface VoteRepository extends JpaRepository<MovieVote, MovieVoteId> {

	@Modifying
	@Transactional
	@Query(value = """
			INSERT INTO movie_vote (user_id, movie_id, liked)
			VALUES (:userId, :movieId, :liked)
			ON CONFLICT (user_id, movie_id)
			DO UPDATE SET liked = EXCLUDED.liked, voted_at = now()
			""", nativeQuery = true)
	void upsertVote(@Param("userId") long userId, @Param("movieId") long movieId, @Param("liked") boolean liked);

	@Query("""
			select vote from MovieVote vote
			where vote.id.userId = :userId
			order by vote.id.movieId
			""")
	List<MovieVote> findVotesByUserId(@Param("userId") long userId);

}
