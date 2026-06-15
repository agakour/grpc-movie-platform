package com.movieplatform.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.movieplatform.user.entity.MovieVote;
import com.movieplatform.user.entity.MovieVoteId;
import com.movieplatform.user.testutil.PostgresTestSupport;

@Testcontainers
@DataJpaTest
@SuppressWarnings("unused")
class VoteRepositoryIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestSupport.POSTGRES_IMAGE);

	@Autowired
	private VoteRepository voteRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void upsertInsertsAndOverwritesInsteadOfAppending() {
		voteRepository.upsertVote(1L, 100L, true);

		entityManager.clear();
		MovieVote vote = voteRepository.findById(new MovieVoteId(1L, 100L)).orElseThrow();
		assertThat(vote.isLiked()).isTrue();
		assertThat(vote.getVotedAt()).isNotNull();

		voteRepository.upsertVote(1L, 100L, false);

		assertThat(voteRepository.count()).isEqualTo(1L);

		entityManager.clear();
		MovieVote overwritten = voteRepository.findById(new MovieVoteId(1L, 100L)).orElseThrow();
		assertThat(overwritten.isLiked()).isFalse();
		assertThat(overwritten.getVotedAt()).isNotNull();
	}

	@Test
	void votesAreScopedAndOrderedPerUser() {
		voteRepository.upsertVote(1L, 200L, true);
		voteRepository.upsertVote(1L, 100L, false);
		voteRepository.upsertVote(2L, 100L, true);

		List<MovieVote> userOneVotes = voteRepository.findVotesByUserId(1L);
		assertThat(userOneVotes).extracting(vote -> vote.getId().getMovieId())
				.containsExactly(100L, 200L);
		assertThat(userOneVotes).extracting(MovieVote::isLiked)
				.containsExactly(false, true);

		assertThat(voteRepository.findVotesByUserId(2L)).hasSize(1);
		assertThat(voteRepository.findVotesByUserId(99L)).isEmpty();
	}

}
