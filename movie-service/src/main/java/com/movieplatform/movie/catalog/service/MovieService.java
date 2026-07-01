package com.movieplatform.movie.catalog.service;
import com.movieplatform.movie.catalog.mapper.MovieMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional
@SuppressWarnings("unused")
public class MovieService {

	private final MovieRepository movieRepository;
	private final MovieDirectorRepository movieDirectorRepository;
	private final MovieCastRepository movieCastRepository;
	private final MovieGenreRepository movieGenreRepository;
	private final MovieKeywordRepository movieKeywordRepository;
	private final PersonRepository personRepository;
	private final GenreRepository genreRepository;
	private final KeywordRepository keywordRepository;
	private final VocabularyService vocabularyService;
	private final MovieMapper movieMapper;

	public MovieService(MovieRepository movieRepository,
			MovieDirectorRepository movieDirectorRepository,
			MovieCastRepository movieCastRepository,
			MovieGenreRepository movieGenreRepository,
			MovieKeywordRepository movieKeywordRepository,
			PersonRepository personRepository,
			GenreRepository genreRepository,
			KeywordRepository keywordRepository,
			VocabularyService vocabularyService,
			MovieMapper movieMapper) {
		this.movieRepository = movieRepository;
		this.movieDirectorRepository = movieDirectorRepository;
		this.movieCastRepository = movieCastRepository;
		this.movieGenreRepository = movieGenreRepository;
		this.movieKeywordRepository = movieKeywordRepository;
		this.personRepository = personRepository;
		this.genreRepository = genreRepository;
		this.keywordRepository = keywordRepository;
		this.vocabularyService = vocabularyService;
		this.movieMapper = movieMapper;
	}

	public MovieResponse create(MovieRequest request) {
		Movie movie = movieMapper.toEntity(request);
		try {
			movie = movieRepository.saveAndFlush(movie);
			Vocabulary vocabulary = resolveVocabulary(request);
			replaceJunctions(movie.getId(), vocabulary.directors(), vocabulary.cast(),
					vocabulary.genres(), vocabulary.keywords());
			return toResponse(movie, vocabulary.directors(), vocabulary.cast(),
					vocabulary.genres(), vocabulary.keywords());
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateMovieException(DuplicateMovieException.constraintOf(exception));
		}
	}

	public MovieResponse update(Long id, MovieRequest request) {
		Movie movie = movieRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("movie", id));
		try {
			movieMapper.applyToEntity(movie, request);
			Vocabulary vocabulary = resolveVocabulary(request);
			movieRepository.saveAndFlush(movie);
			replaceJunctions(movie.getId(), vocabulary.directors(), vocabulary.cast(),
					vocabulary.genres(), vocabulary.keywords());
			return toResponse(movie, vocabulary.directors(), vocabulary.cast(),
					vocabulary.genres(), vocabulary.keywords());
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateMovieException(DuplicateMovieException.constraintOf(exception));
		}
	}

	public void delete(Long id) {
		if (!movieRepository.existsById(id)) {
			throw new NotFoundException("movie", id);
		}
		movieRepository.deleteById(id);
	}

	public MovieResponse get(Long id) {
		Movie movie = movieRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("movie", id));
		List<MovieDirector> directorRows = movieDirectorRepository.findByMovieIdOrderByPosition(id);
		List<MovieCast> castRows = movieCastRepository.findByMovieIdOrderByPosition(id);
		List<MovieGenre> genreRows = movieGenreRepository.findByMovieIdOrderByPosition(id);
		List<MovieKeyword> keywordRows = movieKeywordRepository.findByMovieIdOrderByPosition(id);

		Set<Integer> personIds = new HashSet<>();
		directorRows.forEach(row -> personIds.add(row.getPersonId()));
		castRows.forEach(row -> personIds.add(row.getPersonId()));
		Map<Integer, String> personNames = personRepository.findAllById(personIds).stream()
				.collect(Collectors.toMap(Person::getId, Person::getName));
		Map<Integer, String> genreNames = genreRepository
				.findAllById(genreRows.stream().map(MovieGenre::getGenreId).collect(Collectors.toSet())).stream()
				.collect(Collectors.toMap(Genre::getId, Genre::getName));
		Map<Integer, String> keywordNames = keywordRepository
				.findAllById(keywordRows.stream().map(MovieKeyword::getKeywordId).collect(Collectors.toSet())).stream()
				.collect(Collectors.toMap(Keyword::getId, Keyword::getName));

		return movieMapper.toResponse(movie, directorRows, castRows, genreRows, keywordRows,
				personNames, genreNames, keywordNames);
	}

	public PageResponse<MovieSummaryResponse> list(MovieListQuery query) {
		String qPattern = likePattern(query.q());
		List<String> actorPatterns = personPatterns(query.actor());
		List<String> directorPatterns = personPatterns(query.director());
		Sort.Order order = new Sort.Order(direction(query.dir()), sortProperty(query.sortBy()),
				Sort.NullHandling.NULLS_LAST);
		Pageable pageable = PageRequest.of(query.page(), query.size(), Sort.by(order));
		Page<Movie> page = movieRepository.findFiltered(
				query.genre(),
				pattern(actorPatterns, 0), pattern(actorPatterns, 1), pattern(actorPatterns, 2),
				pattern(actorPatterns, 3), pattern(actorPatterns, 4),
				pattern(directorPatterns, 0), pattern(directorPatterns, 1), pattern(directorPatterns, 2),
				pattern(directorPatterns, 3), pattern(directorPatterns, 4),
				query.keyword(), query.year(), qPattern, pageable);
		return new PageResponse<>(
				page.getContent().stream().map(movieMapper::toSummary).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}

	private String sortProperty(String sortBy) {
		return switch (sortBy) {
			case MovieListQuery.SORT_TITLE -> "title";
			case MovieListQuery.SORT_RELEASE_YEAR -> "releaseYear";
			case MovieListQuery.SORT_RUNTIME_MINUTES -> "runtimeMinutes";
			default -> throw new IllegalArgumentException("Unknown sortBy: '" + sortBy + "'");
		};
	}

	private Sort.Direction direction(String dir) {
		return switch (dir) {
			case MovieListQuery.DIR_ASC -> Sort.Direction.ASC;
			case MovieListQuery.DIR_DESC -> Sort.Direction.DESC;
			default -> throw new IllegalArgumentException("Unknown dir: '" + dir + "'");
		};
	}

	private String likePattern(String q) {
		if (q == null || q.isBlank()) {
			return null;
		}
		return wordPattern(q.trim());
	}

	private static final int MAX_PERSON_TOKENS = 5;

	private List<String> personPatterns(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return Arrays.stream(value.trim().split("\\s+"))
				.limit(MAX_PERSON_TOKENS)
				.map(MovieService::wordPattern)
				.toList();
	}

	private static String wordPattern(String token) {
		if (token.isEmpty()) {
			return token;
		}
		String prefix = isWordCharacter(token.charAt(0)) ? "\\m" : "";
		String suffix = isWordCharacter(token.charAt(token.length() - 1)) ? "\\M" : "";
		return prefix + regexQuote(token) + suffix;
	}

	private static boolean isWordCharacter(char c) {
		return c == '_' || Character.isLetterOrDigit(c);
	}

	private static final String REGEX_META = ".^$|()[]*+?{}\\";

	private static String regexQuote(String value) {
		StringBuilder quoted = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (REGEX_META.indexOf(c) >= 0) {
				quoted.append('\\');
			}
			quoted.append(c);
		}
		return quoted.toString();
	}

	private static String pattern(List<String> patterns, int index) {
		return index < patterns.size() ? patterns.get(index) : null;
	}

	private Vocabulary resolveVocabulary(MovieRequest request) {
		return new Vocabulary(
				vocabularyService.resolvePersons(request.directors()),
				vocabularyService.resolvePersons(request.cast()),
				vocabularyService.resolveGenres(request.genres()),
				vocabularyService.resolveKeywords(request.keywords()));
	}

	private void replaceJunctions(Long movieId, List<Person> directors, List<Person> cast,
			List<Genre> genres, List<Keyword> keywords) {
		movieDirectorRepository.deleteByMovieId(movieId);
		movieCastRepository.deleteByMovieId(movieId);
		movieGenreRepository.deleteByMovieId(movieId);
		movieKeywordRepository.deleteByMovieId(movieId);
		movieDirectorRepository.saveAll(directorRows(movieId, directors));
		movieCastRepository.saveAll(castRows(movieId, cast));
		movieGenreRepository.saveAll(genreRows(movieId, genres));
		movieKeywordRepository.saveAll(keywordRows(movieId, keywords));
	}

	private List<MovieDirector> directorRows(Long movieId, List<Person> directors) {
		List<MovieDirector> rows = new ArrayList<>(directors.size());
		for (int i = 0; i < directors.size(); i++) {
			MovieDirector row = new MovieDirector();
			row.setMovieId(movieId);
			row.setPersonId(directors.get(i).getId());
			row.setPosition((short) (i + 1));
			rows.add(row);
		}
		return rows;
	}

	private List<MovieCast> castRows(Long movieId, List<Person> cast) {
		List<MovieCast> rows = new ArrayList<>(cast.size());
		for (int i = 0; i < cast.size(); i++) {
			MovieCast row = new MovieCast();
			row.setMovieId(movieId);
			row.setPersonId(cast.get(i).getId());
			row.setPosition((short) (i + 1));
			rows.add(row);
		}
		return rows;
	}

	private List<MovieGenre> genreRows(Long movieId, List<Genre> genres) {
		List<MovieGenre> rows = new ArrayList<>(genres.size());
		for (int i = 0; i < genres.size(); i++) {
			MovieGenre row = new MovieGenre();
			row.setMovieId(movieId);
			row.setGenreId(genres.get(i).getId());
			row.setPosition((short) (i + 1));
			rows.add(row);
		}
		return rows;
	}

	private List<MovieKeyword> keywordRows(Long movieId, List<Keyword> keywords) {
		List<MovieKeyword> rows = new ArrayList<>(keywords.size());
		for (int i = 0; i < keywords.size(); i++) {
			MovieKeyword row = new MovieKeyword();
			row.setMovieId(movieId);
			row.setKeywordId(keywords.get(i).getId());
			row.setPosition((short) (i + 1));
			rows.add(row);
		}
		return rows;
	}

	private MovieResponse toResponse(Movie movie, List<Person> directors, List<Person> cast,
			List<Genre> genres, List<Keyword> keywords) {
		Map<Integer, String> personNames = new HashMap<>();
		directors.forEach(person -> personNames.put(person.getId(), person.getName()));
		cast.forEach(person -> personNames.put(person.getId(), person.getName()));
		Map<Integer, String> genreNames = genres.stream()
				.collect(Collectors.toMap(Genre::getId, Genre::getName));
		Map<Integer, String> keywordNames = keywords.stream()
				.collect(Collectors.toMap(Keyword::getId, Keyword::getName));
		return movieMapper.toResponse(movie,
				directorRows(movie.getId(), directors),
				castRows(movie.getId(), cast),
				genreRows(movie.getId(), genres),
				keywordRows(movie.getId(), keywords),
				personNames, genreNames, keywordNames);
	}

	private record Vocabulary(List<Person> directors, List<Person> cast,
			List<Genre> genres, List<Keyword> keywords) {
	}

}
