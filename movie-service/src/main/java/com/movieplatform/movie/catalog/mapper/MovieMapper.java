package com.movieplatform.movie.catalog.mapper;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.dto.MovieSummaryResponse;
import com.movieplatform.movie.catalog.entity.Movie;
import com.movieplatform.movie.catalog.entity.MovieCast;
import com.movieplatform.movie.catalog.entity.MovieDirector;
import com.movieplatform.movie.catalog.entity.MovieGenre;
import com.movieplatform.movie.catalog.entity.MovieKeyword;

@Component
public class MovieMapper {

	public Movie toEntity(MovieRequest request) {
		Movie movie = new Movie();
		applyToEntity(movie, request);
		return movie;
	}

	public void applyToEntity(Movie movie, MovieRequest request) {
		movie.setTitle(request.title());
		movie.setOriginalTitle(request.originalTitle());
		movie.setOverview(request.overview());
		movie.setReleaseYear(request.releaseYear() == null ? null : request.releaseYear().shortValue());
		movie.setRuntimeMinutes(request.runtimeMinutes() == null ? null : request.runtimeMinutes().shortValue());
		movie.setOriginalLanguage(request.originalLanguage());
		movie.setPosterPath(request.posterPath());
		movie.setBackdropPath(request.backdropPath());
		movie.setTmdbId(request.tmdbId());
	}

	public MovieResponse toResponse(Movie movie,
			List<MovieDirector> directorRows,
			List<MovieCast> castRows,
			List<MovieGenre> genreRows,
			List<MovieKeyword> keywordRows,
			Map<Integer, String> personNames,
			Map<Integer, String> genreNames,
			Map<Integer, String> keywordNames) {
		return new MovieResponse(
				movie.getId(),
				movie.getTitle(),
				movie.getOriginalTitle(),
				movie.getOverview(),
				movie.getReleaseYear() == null ? null : movie.getReleaseYear().intValue(),
				movie.getRuntimeMinutes() == null ? null : movie.getRuntimeMinutes().intValue(),
				trimChar(movie.getOriginalLanguage()),
				movie.getPosterPath(),
				movie.getBackdropPath(),
				movie.getTmdbId(),
				directorRows.stream().map(row -> personNames.get(row.getPersonId())).toList(),
				castRows.stream().map(row -> personNames.get(row.getPersonId())).toList(),
				genreRows.stream().map(row -> genreNames.get(row.getGenreId())).toList(),
				keywordRows.stream().map(row -> keywordNames.get(row.getKeywordId())).toList());
	}

	public MovieSummaryResponse toSummary(Movie movie) {
		return new MovieSummaryResponse(
				movie.getId(),
				movie.getTitle(),
				movie.getOriginalTitle(),
				movie.getReleaseYear() == null ? null : movie.getReleaseYear().intValue(),
				movie.getRuntimeMinutes() == null ? null : movie.getRuntimeMinutes().intValue(),
				trimChar(movie.getOriginalLanguage()),
				movie.getPosterPath());
	}

	private String trimChar(String value) {
		return value == null ? null : value.trim();
	}

}
