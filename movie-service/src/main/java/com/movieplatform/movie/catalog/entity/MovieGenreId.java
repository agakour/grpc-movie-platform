package com.movieplatform.movie.catalog.entity;

import java.io.Serializable;
import java.util.Objects;

public class MovieGenreId implements Serializable {

	private Long movieId;

	private Short position;

	public MovieGenreId() {
	}

	public MovieGenreId(Long movieId, Short position) {
		this.movieId = movieId;
		this.position = position;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MovieGenreId that)) {
			return false;
		}
		return Objects.equals(movieId, that.movieId) && Objects.equals(position, that.position);
	}

	@Override
	public int hashCode() {
		return Objects.hash(movieId, position);
	}

}
