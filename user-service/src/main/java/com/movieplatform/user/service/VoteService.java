package com.movieplatform.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.movieplatform.user.entity.MovieVote;
import com.movieplatform.user.entity.MovieVoteId;
import com.movieplatform.user.repository.VoteRepository;

@Service
@SuppressWarnings("unused")
public class VoteService {

	private final VoteRepository voteRepository;

	public VoteService(VoteRepository voteRepository) {
		this.voteRepository = voteRepository;
	}

	public void setVote(long userId, long movieId, boolean liked) {
		voteRepository.upsertVote(userId, movieId, liked);
	}

	public Optional<Boolean> getVote(long userId, long movieId) {
		return voteRepository.findById(new MovieVoteId(userId, movieId)).map(MovieVote::isLiked);
	}

	public List<Vote> listVotes(long userId) {
		return voteRepository.findVotesByUserId(userId).stream()
				.map(vote -> new Vote(vote.getId().getMovieId(), vote.isLiked()))
				.toList();
	}

}
