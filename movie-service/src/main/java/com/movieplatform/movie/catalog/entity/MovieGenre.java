package com.movieplatform.movie.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "movie_genre")
@IdClass(MovieGenreId.class)
@SuppressWarnings("unused")
public class MovieGenre {

	@Id
	@Column(name = "movie_id", nullable = false)
	private Long movieId;

	@Id
	@Column(name = "position", nullable = false)
	private Short position;

	@Column(name = "genre_id", nullable = false)
	private Integer genreId;

	public Long getMovieId() {
		return movieId;
	}

	public void setMovieId(Long movieId) {
		this.movieId = movieId;
	}

	public Short getPosition() {
		return position;
	}

	public void setPosition(Short position) {
		this.position = position;
	}

	public Integer getGenreId() {
		return genreId;
	}

	public void setGenreId(Integer genreId) {
		this.genreId = genreId;
	}

}
