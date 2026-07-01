package com.movieplatform.movie.catalog.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MovieRequest(

		@NotBlank
		@Size(max = 500)
		String title,

		@NotBlank
		@Size(max = 500)
		String originalTitle,

		String overview,

		@NotNull
		@Min(1888)
		@Max(2100)
		Integer releaseYear,

		@NotNull
		@Min(1)
		@Max(1000)
		Integer runtimeMinutes,

		@Pattern(regexp = "^[a-z]{2}$")
		String originalLanguage,

		String posterPath,

		String backdropPath,

		@NotNull
		@Min(1)
		Long tmdbId,

		List<String> directors,

		List<String> cast,

		List<String> genres,

		List<String> keywords) {

}
