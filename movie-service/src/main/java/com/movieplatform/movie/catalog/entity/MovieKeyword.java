package com.movieplatform.movie.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "movie_keyword")
@IdClass(MovieKeywordId.class)
@SuppressWarnings("unused")
public class MovieKeyword {

	@Id
	@Column(name = "movie_id", nullable = false)
	private Long movieId;

	@Id
	@Column(name = "position", nullable = false)
	private Short position;

	@Column(name = "keyword_id", nullable = false)
	private Integer keywordId;

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

	public Integer getKeywordId() {
		return keywordId;
	}

	public void setKeywordId(Integer keywordId) {
		this.keywordId = keywordId;
	}

}
