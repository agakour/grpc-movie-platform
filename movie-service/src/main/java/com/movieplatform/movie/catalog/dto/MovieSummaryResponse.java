package com.movieplatform.movie.catalog.dto;

public record MovieSummaryResponse(

		Long id,

		String title,

		String originalTitle,

		Integer releaseYear,

		Integer runtimeMinutes,

		String originalLanguage,

		String posterPath) {

}
