package com.movieplatform.movie.catalog.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movie")
@SuppressWarnings("unused")
public class Movie {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "original_title", nullable = false)
	private String originalTitle;

	@Column(name = "overview")
	private String overview;

	@Column(name = "release_year", nullable = false)
	private Short releaseYear;

	@Column(name = "runtime_minutes", nullable = false)
	private Short runtimeMinutes;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "original_language", length = 2)
	private String originalLanguage;

	@Column(name = "poster_path")
	private String posterPath;

	@Column(name = "backdrop_path")
	private String backdropPath;

	@Column(name = "tmdb_id", nullable = false, unique = true)
	private Long tmdbId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getOriginalTitle() {
		return originalTitle;
	}

	public void setOriginalTitle(String originalTitle) {
		this.originalTitle = originalTitle;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public Short getReleaseYear() {
		return releaseYear;
	}

	public void setReleaseYear(Short releaseYear) {
		this.releaseYear = releaseYear;
	}

	public Short getRuntimeMinutes() {
		return runtimeMinutes;
	}

	public void setRuntimeMinutes(Short runtimeMinutes) {
		this.runtimeMinutes = runtimeMinutes;
	}

	public String getOriginalLanguage() {
		return originalLanguage;
	}

	public void setOriginalLanguage(String originalLanguage) {
		this.originalLanguage = originalLanguage;
	}

	public String getPosterPath() {
		return posterPath;
	}

	public void setPosterPath(String posterPath) {
		this.posterPath = posterPath;
	}

	public String getBackdropPath() {
		return backdropPath;
	}

	public void setBackdropPath(String backdropPath) {
		this.backdropPath = backdropPath;
	}

	public Long getTmdbId() {
		return tmdbId;
	}

	public void setTmdbId(Long tmdbId) {
		this.tmdbId = tmdbId;
	}

}
