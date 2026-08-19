package com.movieplatform.movie.remote.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbSearchResult(
		long id,
		String title,
		@JsonProperty("original_title") String originalTitle,
		@JsonProperty("release_date") String releaseDate,
		@JsonProperty("poster_path") String posterPath,
		@JsonProperty("backdrop_path") String backdropPath,
		String overview,
		@JsonProperty("original_language") String originalLanguage) {
}
