package com.movieplatform.movie.web.admin;

import java.util.Arrays;
import java.util.List;

import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.remote.tmdb.TmdbMovieEnrichment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminMovieForm(

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

		@Pattern(regexp = "^([a-z]{2})?$")
		String originalLanguage,

		String posterPath,

		String backdropPath,

		@NotNull
		@Min(1)
		Long tmdbId,

		String directors,

		String cast,

		String genres,

		String keywords,

		String action) {

	public static AdminMovieForm empty() {
		return new AdminMovieForm("", "", "", null, null, "", "", "", null, "", "", "", "", null);
	}

	public static AdminMovieForm fromResponse(MovieResponse movie) {
		return new AdminMovieForm(
				movie.title(),
				movie.originalTitle(),
				movie.overview(),
				movie.releaseYear(),
				movie.runtimeMinutes(),
				movie.originalLanguage() == null ? "" : movie.originalLanguage(),
				movie.posterPath() == null ? "" : movie.posterPath(),
				movie.backdropPath() == null ? "" : movie.backdropPath(),
				movie.tmdbId(),
				String.join("\n", movie.directors()),
				String.join("\n", movie.cast()),
				String.join("\n", movie.genres()),
				String.join("\n", movie.keywords()),
				null);
	}

	public AdminMovieForm withFullBackfill(TmdbMovieEnrichment enrichment) {
		return new AdminMovieForm(
				enrichment.title() == null ? title : enrichment.title(),
				enrichment.originalTitle() == null ? originalTitle : enrichment.originalTitle(),
				enrichment.overview() == null ? overview : enrichment.overview(),
				enrichment.releaseYear() == null ? releaseYear : enrichment.releaseYear(),
				enrichment.runtimeMinutes() == null ? runtimeMinutes : enrichment.runtimeMinutes(),
				enrichment.originalLanguage() == null ? originalLanguage : enrichment.originalLanguage(),
				enrichment.posterPath() == null ? posterPath : enrichment.posterPath(),
				enrichment.backdropPath() == null ? backdropPath : enrichment.backdropPath(),
				enrichment.tmdbId(),
				backfillLines(directors, enrichment.directors()),
				backfillLines(cast, enrichment.cast()),
				backfillLines(genres, enrichment.genres()),
				backfillLines(keywords, enrichment.keywords()),
				action);
	}

	private static String backfillLines(String current, List<String> values) {
		if (values == null || values.isEmpty()) {
			return current;
		}
		return String.join("\n", values);
	}

	public MovieRequest toRequest() {
		return new MovieRequest(
				title,
				originalTitle,
				blankToNull(overview),
				releaseYear,
				runtimeMinutes,
				blankToNull(originalLanguage),
				blankToNull(posterPath),
				blankToNull(backdropPath),
				tmdbId,
				lines(directors),
				lines(cast),
				lines(genres),
				lines(keywords));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static List<String> lines(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return Arrays.stream(value.split("\\R"))
				.map(String::trim)
				.filter(line -> !line.isEmpty())
				.toList();
	}

}
