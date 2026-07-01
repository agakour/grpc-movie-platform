package com.movieplatform.movie.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.entity.Movie;
import com.movieplatform.movie.catalog.entity.MovieCast;
import com.movieplatform.movie.catalog.entity.MovieDirector;
import com.movieplatform.movie.catalog.entity.MovieGenre;
import com.movieplatform.movie.catalog.entity.MovieKeyword;

@SuppressWarnings("unused")
class MovieMapperTest {

	private final MovieMapper mapper = new MovieMapper();

	@Test
	void toEntityMapsScalarFieldsWithShortConversion() {
		MovieRequest request = new MovieRequest("Title", "Original", "Overview", 1999, 120, "en",
				"/p.jpg", "/b.jpg", 123L, null, null, null, null);

		Movie movie = mapper.toEntity(request);

		assertThat(movie.getId()).isNull();
		assertThat(movie.getTitle()).isEqualTo("Title");
		assertThat(movie.getOriginalTitle()).isEqualTo("Original");
		assertThat(movie.getOverview()).isEqualTo("Overview");
		assertThat(movie.getReleaseYear()).isEqualTo((short) 1999);
		assertThat(movie.getRuntimeMinutes()).isEqualTo((short) 120);
		assertThat(movie.getOriginalLanguage()).isEqualTo("en");
		assertThat(movie.getPosterPath()).isEqualTo("/p.jpg");
		assertThat(movie.getBackdropPath()).isEqualTo("/b.jpg");
		assertThat(movie.getTmdbId()).isEqualTo(123L);
	}

	@Test
	void toEntityMapsTmdbIdAndLeavesOptionalScalarsNullable() {
		MovieRequest request = new MovieRequest("Title", "Original", null, 1999, 120, null,
				null, null, 123L, null, null, null, null);

		Movie movie = mapper.toEntity(request);

		assertThat(movie.getOverview()).isNull();
		assertThat(movie.getOriginalLanguage()).isNull();
		assertThat(movie.getPosterPath()).isNull();
		assertThat(movie.getBackdropPath()).isNull();
		assertThat(movie.getReleaseYear()).isEqualTo((short) 1999);
		assertThat(movie.getRuntimeMinutes()).isEqualTo((short) 120);
		assertThat(movie.getTmdbId()).isEqualTo(123L);
	}

	@Test
	void toResponseAssemblesOrderedArraysFromJunctionRows() {
		Movie movie = new Movie();
		movie.setId(7L);
		movie.setTitle("T");
		movie.setOriginalTitle("O");
		movie.setOverview("ov");
		movie.setReleaseYear((short) 2001);
		movie.setRuntimeMinutes((short) 90);
		movie.setOriginalLanguage("en");
		movie.setPosterPath("/p");
		movie.setBackdropPath("/b");
		movie.setTmdbId(123L);

		List<MovieDirector> directors = List.of(row(1, 1), row(2, 2));
		List<MovieCast> cast = List.of(castRow(3, 1));
		List<MovieGenre> genres = List.of(genreRow(4, 1));
		List<MovieKeyword> keywords = List.of();

		MovieResponse response = mapper.toResponse(movie, directors, cast, genres, keywords,
				Map.of(1, "Dir One", 2, "Dir Two", 3, "Actor One"),
				Map.of(4, "drama"),
				Map.of());

		assertThat(response.id()).isEqualTo(7L);
		assertThat(response.releaseYear()).isEqualTo(2001);
		assertThat(response.runtimeMinutes()).isEqualTo(90);
		assertThat(response.originalLanguage()).isEqualTo("en");
		assertThat(response.tmdbId()).isEqualTo(123L);
		assertThat(response.directors()).containsExactly("Dir One", "Dir Two");
		assertThat(response.cast()).containsExactly("Actor One");
		assertThat(response.genres()).containsExactly("drama");
		assertThat(response.keywords()).isEmpty();
	}

	@Test
	void toResponseTrimsPaddedCharLanguage() {
		Movie movie = new Movie();
		movie.setId(1L);
		movie.setOriginalLanguage("e ");

		MovieResponse response = mapper.toResponse(movie, List.of(), List.of(), List.of(), List.of(),
				Map.of(), Map.of(), Map.of());

		assertThat(response.originalLanguage()).isEqualTo("e");
	}

	@Test
	void roundTripPreservesListContentAndOrder() {
		MovieRequest request = new MovieRequest("Round Trip", "Round Trip Original", "overview",
				1988, 100, "fr", "/p.jpg", null, 321L,
				List.of("Director B", "Director A"),
				List.of("Actor X", "Actor Y", "Actor Z"),
				List.of("Drama", "Action"),
				List.of("kw-one", "kw-two"));

		Movie movie = mapper.toEntity(request);
		Map<Integer, String> personNames = Map.of(1, "Director B", 2, "Director A", 3, "Actor X", 4, "Actor Y", 5, "Actor Z");
		Map<Integer, String> genreNames = Map.of(6, "Drama", 7, "Action");
		Map<Integer, String> keywordNames = Map.of(8, "kw-one", 9, "kw-two");

		MovieResponse response = mapper.toResponse(movie,
				List.of(row(1, 1), row(2, 2)),
				List.of(castRow(3, 1), castRow(4, 2), castRow(5, 3)),
				List.of(genreRow(6, 1), genreRow(7, 2)),
				List.of(keywordRow(8, 1), keywordRow(9, 2)),
				personNames, genreNames, keywordNames);

		assertThat(response.title()).isEqualTo(request.title());
		assertThat(response.originalTitle()).isEqualTo(request.originalTitle());
		assertThat(response.releaseYear()).isEqualTo(request.releaseYear());
		assertThat(response.runtimeMinutes()).isEqualTo(request.runtimeMinutes());
		assertThat(response.originalLanguage()).isEqualTo(request.originalLanguage());
		assertThat(response.tmdbId()).isEqualTo(request.tmdbId());
		assertThat(response.directors()).containsExactlyElementsOf(request.directors());
		assertThat(response.cast()).containsExactlyElementsOf(request.cast());
		assertThat(response.genres()).containsExactlyElementsOf(request.genres());
		assertThat(response.keywords()).containsExactlyElementsOf(request.keywords());
	}

	private static MovieDirector row(int personId, int position) {
		MovieDirector row = new MovieDirector();
		row.setMovieId(1L);
		row.setPersonId(personId);
		row.setPosition((short) position);
		return row;
	}

	private static MovieCast castRow(int personId, int position) {
		MovieCast row = new MovieCast();
		row.setMovieId(1L);
		row.setPersonId(personId);
		row.setPosition((short) position);
		return row;
	}

	private static MovieGenre genreRow(int genreId, int position) {
		MovieGenre row = new MovieGenre();
		row.setMovieId(1L);
		row.setGenreId(genreId);
		row.setPosition((short) position);
		return row;
	}

	private static MovieKeyword keywordRow(int keywordId, int position) {
		MovieKeyword row = new MovieKeyword();
		row.setMovieId(1L);
		row.setKeywordId(keywordId);
		row.setPosition((short) position);
		return row;
	}

}
