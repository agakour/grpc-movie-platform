package com.movieplatform.user.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
@SuppressWarnings("unused")
public class MovieVoteId implements Serializable {

	@Column(name = "user_id", nullable = false)
	private long userId;

	@Column(name = "movie_id", nullable = false)
	private long movieId;

	public MovieVoteId() {
	}

	public MovieVoteId(long userId, long movieId) {
		this.userId = userId;
		this.movieId = movieId;
	}

	public long getMovieId() {
		return movieId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MovieVoteId that)) {
			return false;
		}
		return userId == that.userId && movieId == that.movieId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, movieId);
	}

}
