package com.movieplatform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.movieplatform.user.entity.MovieVote;
import com.movieplatform.user.entity.MovieVoteId;
import com.movieplatform.user.repository.VoteRepository;

@SuppressWarnings("unused")
class VoteServiceTest {

	private final VoteRepository voteRepository = mock(VoteRepository.class);

	private final VoteService voteService = new VoteService(voteRepository);

	@Test
	void setVoteDelegatesToUpsert() {
		voteService.setVote(1L, 100L, true);

		verify(voteRepository).upsertVote(1L, 100L, true);
	}

	@Test
	void getVoteReturnsLikedWhenPresent() {
		MovieVote vote = new MovieVote();
		vote.setId(new MovieVoteId(1L, 100L));
		vote.setLiked(false);
		when(voteRepository.findById(new MovieVoteId(1L, 100L))).thenReturn(Optional.of(vote));

		assertThat(voteService.getVote(1L, 100L)).contains(false);
	}

	@Test
	void getVoteReturnsEmptyWhenAbsent() {
		when(voteRepository.findById(new MovieVoteId(1L, 100L))).thenReturn(Optional.empty());

		assertThat(voteService.getVote(1L, 100L)).isEmpty();
	}

	@Test
	void listVotesMapsEntitiesToDtos() {
		MovieVote first = new MovieVote();
		first.setId(new MovieVoteId(1L, 100L));
		first.setLiked(true);
		MovieVote second = new MovieVote();
		second.setId(new MovieVoteId(1L, 200L));
		second.setLiked(false);
		when(voteRepository.findVotesByUserId(1L)).thenReturn(List.of(first, second));

		assertThat(voteService.listVotes(1L))
				.containsExactly(new Vote(100L, true), new Vote(200L, false));
	}

}
