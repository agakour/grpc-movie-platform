package com.movieplatform.movie.remote.grpc;

public record RecommendationItem(
		long tmdbId,
		String title,
		int year,
		String posterPath,
		String overview,
		double score) {
}
