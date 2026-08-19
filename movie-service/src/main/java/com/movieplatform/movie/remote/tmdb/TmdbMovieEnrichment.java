package com.movieplatform.movie.remote.tmdb;

import java.util.List;

public record TmdbMovieEnrichment(
		long tmdbId,
		String title,
		String originalTitle,
		String overview,
		Integer releaseYear,
		Integer runtimeMinutes,
		String originalLanguage,
		String posterPath,
		String backdropPath,
		List<String> directors,
		List<String> cast,
		List<String> genres,
		List<String> keywords) {
}
