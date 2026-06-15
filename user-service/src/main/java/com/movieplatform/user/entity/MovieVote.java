package com.movieplatform.user.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "movie_vote")
@SuppressWarnings("unused")
public class MovieVote {

	@EmbeddedId
	private MovieVoteId id;

	@Column(name = "liked", nullable = false)
	private boolean liked;

	@Column(name = "voted_at", nullable = false, insertable = false, updatable = false)
	private Instant votedAt;

	public MovieVoteId getId() {
		return id;
	}

	public void setId(MovieVoteId id) {
		this.id = id;
	}

	public boolean isLiked() {
		return liked;
	}

	public void setLiked(boolean liked) {
		this.liked = liked;
	}

	public Instant getVotedAt() {
		return votedAt;
	}

}
