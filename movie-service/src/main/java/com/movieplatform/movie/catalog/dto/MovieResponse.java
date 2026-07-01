package com.movieplatform.movie.catalog.dto;

import java.util.List;

public record MovieResponse(

		Long id,

		String title,

		String originalTitle,

		String overview,

		Integer releaseYear,

		Integer runtimeMinutes,

		String originalLanguage,

		String posterPath,

		String backdropPath,

		Long tmdbId,

		List<String> directors,

		List<String> cast,

		List<String> genres,

		List<String> keywords) {

}
