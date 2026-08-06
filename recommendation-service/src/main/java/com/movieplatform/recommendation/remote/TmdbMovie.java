package com.movieplatform.recommendation.remote;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovie(
		long id,
		String title,
		@JsonProperty("release_date") String releaseDate,
		@JsonProperty("poster_path") String posterPath,
		String overview,
		@JsonProperty("vote_average") double voteAverage) {
}
