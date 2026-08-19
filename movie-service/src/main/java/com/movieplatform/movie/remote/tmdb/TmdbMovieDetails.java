package com.movieplatform.movie.remote.tmdb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieDetails(
		long id,
		String title,
		@JsonProperty("original_title") String originalTitle,
		@JsonProperty("release_date") String releaseDate,
		Integer runtime,
		@JsonProperty("poster_path") String posterPath,
		@JsonProperty("backdrop_path") String backdropPath,
		String overview,
		@JsonProperty("original_language") String originalLanguage,
		List<Genre> genres) {

	@SuppressWarnings("unused")
	public record Genre(long id, String name) {
	}

}
