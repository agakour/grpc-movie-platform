package com.movieplatform.movie.catalog.service;
import com.movieplatform.movie.catalog.mapper.MovieMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.movieplatform.movie.exception.DuplicateMovieException;
import com.movieplatform.movie.exception.NotFoundException;
import com.movieplatform.movie.catalog.dto.MovieListQuery;
import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.dto.MovieSummaryResponse;
import com.movieplatform.movie.catalog.dto.PageResponse;
import com.movieplatform.movie.catalog.entity.Movie;
import com.movieplatform.movie.catalog.entity.MovieCast;
import com.movieplatform.movie.catalog.repository.MovieCastRepository;
import com.movieplatform.movie.catalog.entity.MovieDirector;
import com.movieplatform.movie.catalog.repository.MovieDirectorRepository;
import com.movieplatform.movie.catalog.entity.MovieGenre;
import com.movieplatform.movie.catalog.repository.MovieGenreRepository;
import com.movieplatform.movie.catalog.entity.MovieKeyword;
import com.movieplatform.movie.catalog.repository.MovieKeywordRepository;
import com.movieplatform.movie.catalog.repository.MovieRepository;
import com.movieplatform.movie.catalog.entity.Genre;
import com.movieplatform.movie.catalog.repository.GenreRepository;
import com.movieplatform.movie.catalog.entity.Keyword;
import com.movieplatform.movie.catalog.repository.KeywordRepository;
import com.movieplatform.movie.catalog.entity.Person;
import com.movieplatform.movie.catalog.repository.PersonRepository;
import com.movieplatform.movie.catalog.service.VocabularyService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class MovieServiceTest {

	@Mock
	private MovieRepository movieRepository;
	@Mock
	private MovieDirectorRepository movieDirectorRepository;
	@Mock
	private MovieCastRepository movieCastRepository;
	@Mock
	private MovieGenreRepository movieGenreRepository;
	@Mock
	private MovieKeywordRepository movieKeywordRepository;
	@Mock
	private PersonRepository personRepository;
	@Mock
	private GenreRepository genreRepository;
	@Mock
	private KeywordRepository keywordRepository;
	@Mock
	private VocabularyService vocabularyService;

	private final MovieMapper movieMapper = new MovieMapper();
	private MovieService service;

	@BeforeEach
	void setUp() {
		service = new MovieService(movieRepository, movieDirectorRepository, movieCastRepository,
				movieGenreRepository, movieKeywordRepository, personRepository, genreRepository,
				keywordRepository, vocabularyService, movieMapper);
	}

	@Test
	void createSavesMovieResolvesListsAndMaintainsJunctions() {
		MovieRequest request = new MovieRequest("New", "New Original", "overview", 2001, 90, "en",
				"/p.jpg", null, 1L, List.of(" Dir One "), List.of(" Actor One "), List.of(" DRAMA "), List.of(" KW ONE "));

		when(movieRepository.saveAndFlush(any(Movie.class))).thenAnswer(invocation -> {
			Movie movie = invocation.getArgument(0);
			movie.setId(42L);
			return movie;
		});
		Person director = new Person("Dir One");
		director.setId(1);
		Person actor = new Person("Actor One");
		actor.setId(2);
		Genre genre = new Genre("drama");
		genre.setId(3);
		Keyword keyword = new Keyword("kw one");
		keyword.setId(4);
		when(vocabularyService.resolvePersons(request.directors())).thenReturn(List.of(director));
		when(vocabularyService.resolvePersons(request.cast())).thenReturn(List.of(actor));
		when(vocabularyService.resolveGenres(request.genres())).thenReturn(List.of(genre));
		when(vocabularyService.resolveKeywords(request.keywords())).thenReturn(List.of(keyword));

		MovieResponse response = service.create(request);

		assertThat(response.id()).isEqualTo(42L);
		assertThat(response.directors()).containsExactly("Dir One");
		assertThat(response.cast()).containsExactly("Actor One");
		assertThat(response.genres()).containsExactly("drama");
		assertThat(response.keywords()).containsExactly("kw one");

		verify(movieDirectorRepository).deleteByMovieId(42L);
		verify(movieCastRepository).deleteByMovieId(42L);
		verify(movieGenreRepository).deleteByMovieId(42L);
		verify(movieKeywordRepository).deleteByMovieId(42L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MovieDirector>> directorRows = ArgumentCaptor.forClass(List.class);
		verify(movieDirectorRepository).saveAll(directorRows.capture());
		assertThat(directorRows.getValue()).hasSize(1);
		MovieDirector directorRow = directorRows.getValue().getFirst();
		assertThat(directorRow.getMovieId()).isEqualTo(42L);
		assertThat(directorRow.getPersonId()).isEqualTo(1);
		assertThat(directorRow.getPosition()).isEqualTo((short) 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MovieKeyword>> keywordRows = ArgumentCaptor.forClass(List.class);
		verify(movieKeywordRepository).saveAll(keywordRows.capture());
		assertThat(keywordRows.getValue()).hasSize(1);
		assertThat(keywordRows.getValue().getFirst().getKeywordId()).isEqualTo(4);
		assertThat(keywordRows.getValue().getFirst().getPosition()).isEqualTo((short) 1);
	}

	@Test
	void createMapsIdentityViolationToDuplicateMovieException() {
		MovieRequest request = minimalRequest();
		when(movieRepository.saveAndFlush(any(Movie.class))).thenThrow(
				new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uq_movie_tmdb_id\""));

		assertThatThrownBy(() -> service.create(request))
				.isInstanceOf(DuplicateMovieException.class)
				.extracting(exception -> ((DuplicateMovieException) exception).getConstraintName())
				.isEqualTo("uq_movie_tmdb_id");
	}

	@Test
	void updateReplacesScalarsAndJunctions() {
		Movie existing = new Movie();
		existing.setId(7L);
		existing.setTitle("Old");
		when(movieRepository.findById(7L)).thenReturn(Optional.of(existing));
		when(vocabularyService.resolvePersons(any())).thenReturn(List.of());
		when(vocabularyService.resolveGenres(any())).thenReturn(List.of());
		when(vocabularyService.resolveKeywords(any())).thenReturn(List.of());

		MovieRequest request = new MovieRequest("Updated", "Updated Original", null, 1990, null,
				null, "/new.jpg", null, 7L, List.of(), List.of(), List.of(), List.of());

		MovieResponse response = service.update(7L, request);

		assertThat(response.title()).isEqualTo("Updated");
		assertThat(response.posterPath()).isEqualTo("/new.jpg");
		assertThat(response.directors()).isEmpty();
		assertThat(existing.getTitle()).isEqualTo("Updated");
		assertThat(existing.getOverview()).isNull();
		assertThat(existing.getReleaseYear()).isEqualTo((short) 1990);
		assertThat(existing.getRuntimeMinutes()).isNull();
		assertThat(existing.getOriginalLanguage()).isNull();

		verify(movieDirectorRepository).deleteByMovieId(7L);
		verify(movieCastRepository).deleteByMovieId(7L);
		verify(movieGenreRepository).deleteByMovieId(7L);
		verify(movieKeywordRepository).deleteByMovieId(7L);
	}

	@Test
	void updateMissingMovieThrowsNotFound() {
		when(movieRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(99L, minimalRequest()))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void deleteMissingMovieThrowsNotFound() {
		when(movieRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(99L))
				.isInstanceOf(NotFoundException.class);
		verify(movieRepository, never()).deleteById(any());
	}

	@Test
	void deleteExistingMovieDelegatesToRepository() {
		when(movieRepository.existsById(5L)).thenReturn(true);

		service.delete(5L);

		verify(movieRepository).deleteById(5L);
	}

	@Test
	void getAssemblesOrderedArraysFromPersistence() {
		Movie movie = new Movie();
		movie.setId(9L);
		movie.setTitle("Probe");
		movie.setOriginalTitle("Probe Original");
		when(movieRepository.findById(9L)).thenReturn(Optional.of(movie));

		MovieDirector first = new MovieDirector();
		first.setMovieId(9L);
		first.setPersonId(1);
		first.setPosition((short) 1);
		MovieDirector second = new MovieDirector();
		second.setMovieId(9L);
		second.setPersonId(2);
		second.setPosition((short) 2);
		when(movieDirectorRepository.findByMovieIdOrderByPosition(9L)).thenReturn(List.of(first, second));

		MovieCast cast = new MovieCast();
		cast.setMovieId(9L);
		cast.setPersonId(3);
		cast.setPosition((short) 1);
		when(movieCastRepository.findByMovieIdOrderByPosition(9L)).thenReturn(List.of(cast));

		MovieGenre genre = new MovieGenre();
		genre.setMovieId(9L);
		genre.setGenreId(4);
		genre.setPosition((short) 1);
		when(movieGenreRepository.findByMovieIdOrderByPosition(9L)).thenReturn(List.of(genre));
		when(movieKeywordRepository.findByMovieIdOrderByPosition(9L)).thenReturn(List.of());

		Person directorOne = new Person("Dir One");
		directorOne.setId(1);
		Person directorTwo = new Person("Dir Two");
		directorTwo.setId(2);
		Person actor = new Person("Actor");
		actor.setId(3);
		when(personRepository.findAllById(Set.of(1, 2, 3)))
				.thenReturn(List.of(directorOne, directorTwo, actor));
		Genre drama = new Genre("drama");
		drama.setId(4);
		when(genreRepository.findAllById(Set.of(4))).thenReturn(List.of(drama));
		when(keywordRepository.findAllById(Set.of())).thenReturn(List.of());

		MovieResponse response = service.get(9L);

		assertThat(response.id()).isEqualTo(9L);
		assertThat(response.directors()).containsExactly("Dir One", "Dir Two");
		assertThat(response.cast()).containsExactly("Actor");
		assertThat(response.genres()).containsExactly("drama");
		assertThat(response.keywords()).isEmpty();
	}

	@Test
	void listBuildsNullsLastSortAndMapsSummaries() {
		MovieListQuery query = new MovieListQuery("drama", null, null, null, null, "samurai",
				"title", "asc", 0, 10);

		Movie movie = new Movie();
		movie.setId(1L);
		movie.setTitle("A Movie");
		movie.setOriginalTitle("A Movie");
		when(movieRepository.findFiltered(eq("drama"),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), eq("\\msamurai\\M"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(movie)));

		PageResponse<MovieSummaryResponse> response = service.list(query);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().title()).isEqualTo("A Movie");
		assertThat(response.totalElements()).isEqualTo(1);
	}

	@Test
	void listPassesEscapedPatternForWildcardCharacters() {
		MovieListQuery query = new MovieListQuery(null, null, null, null, null, "100%",
				"releaseYear", "desc", 0, 20);

		when(movieRepository.findFiltered(isNull(),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), eq("\\m100%"), any(Pageable.class))).thenReturn(Page.empty());

		service.list(query);

		verify(movieRepository).findFiltered(isNull(),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), eq("\\m100%"), any(Pageable.class));
	}

	@Test
	void listTokenizesActorAndDirectorIntoWordBoundaryPatterns() {
		MovieListQuery query = new MovieListQuery(null, "Wes ANDERSON", "Anderson Wes", null, null, null,
				"releaseYear", "desc", 0, 20);

		when(movieRepository.findFiltered(isNull(),
				eq("\\mWes\\M"), eq("\\mANDERSON\\M"), isNull(), isNull(), isNull(),
				eq("\\mAnderson\\M"), eq("\\mWes\\M"), isNull(), isNull(), isNull(),
				isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

		service.list(query);

		verify(movieRepository).findFiltered(isNull(),
				eq("\\mWes\\M"), eq("\\mANDERSON\\M"), isNull(), isNull(), isNull(),
				eq("\\mAnderson\\M"), eq("\\mWes\\M"), isNull(), isNull(), isNull(),
				isNull(), isNull(), isNull(), any(Pageable.class));
	}

	@Test
	void listQuotesRegexMetacharactersInQueryAndPersonTokens() {
		MovieListQuery query = new MovieListQuery(null, "act.or", null, null, null, "a.b",
				"releaseYear", "desc", 0, 20);

		when(movieRepository.findFiltered(isNull(),
				eq("\\mact\\.or\\M"), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), eq("\\ma\\.b\\M"), any(Pageable.class))).thenReturn(Page.empty());

		service.list(query);

		verify(movieRepository).findFiltered(isNull(),
				eq("\\mact\\.or\\M"), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), isNull(), isNull(), isNull(),
				isNull(), isNull(), eq("\\ma\\.b\\M"), any(Pageable.class));
	}

	@Test
	void listRejectsUnknownSortBy() {
		MovieListQuery query = new MovieListQuery(null, null, null, null, null, null,
				"bogus", "asc", 0, 20);

		assertThatThrownBy(() -> service.list(query))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("bogus");
	}

	@Test
	void listRejectsUnknownDir() {
		MovieListQuery query = new MovieListQuery(null, null, null, null, null, null,
				"title", "sideways", 0, 20);

		assertThatThrownBy(() -> service.list(query))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("sideways");
	}

	private static MovieRequest minimalRequest() {
		return new MovieRequest("Title", "Original", null, null, null, null,
				null, null, 1L, null, null, null, null);
	}

}
