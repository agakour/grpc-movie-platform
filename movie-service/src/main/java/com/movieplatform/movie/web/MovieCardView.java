package com.movieplatform.movie.web;

public record MovieCardView(
		Long id,
		String title,
		Integer releaseYear,
		Integer runtimeMinutes,
		String posterUrl) {
}
