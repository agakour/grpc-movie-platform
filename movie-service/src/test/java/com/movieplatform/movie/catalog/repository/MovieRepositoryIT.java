package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.Movie;
import com.movieplatform.movie.catalog.entity.MovieDirector;
import com.movieplatform.movie.catalog.entity.MovieCast;
import com.movieplatform.movie.catalog.entity.MovieGenre;
import com.movieplatform.movie.catalog.entity.MovieKeyword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.movieplatform.movie.exception.DuplicateMovieException;
import com.movieplatform.movie.testutil.PostgresTestSupport;
import com.movieplatform.movie.catalog.entity.Genre;
import com.movieplatform.movie.catalog.repository.GenreRepository;
import com.movieplatform.movie.catalog.entity.Keyword;
import com.movieplatform.movie.catalog.repository.KeywordRepository;
import com.movieplatform.movie.catalog.entity.Person;
import com.movieplatform.movie.catalog.repository.PersonRepository;

@Testcontainers
@DataJpaTest
@SuppressWarnings("unused")
class MovieRepositoryIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestSupport.POSTGRES_IMAGE);

	@Autowired
	private MovieRepository movieRepository;
	@Autowired
	private MovieDirectorRepository movieDirectorRepository;
	@Autowired
	private MovieCastRepository movieCastRepository;
	@Autowired
	private MovieGenreRepository movieGenreRepository;
	@Autowired
	private MovieKeywordRepository movieKeywordRepository;
	@Autowired
	private GenreRepository genreRepository;
	@Autowired
	private PersonRepository personRepository;
	@Autowired
	private KeywordRepository keywordRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void seededCatalogBootsWith1340Movies() {
		assertThat(movieRepository.count()).isEqualTo(1340L);
	}

	@Test
	void duplicateTmdbIdIsRejectedByConstraint() {
		long tmdbId = uniqueTmdbId();
		Movie first = movie("Duplicate TMDb Probe", "Duplicate TMDb Original", (short) 2000, (short) 90);
		first.setTmdbId(tmdbId);
		movieRepository.saveAndFlush(first);

		Movie duplicate = movie("Duplicate TMDb Probe 2", "Duplicate TMDb Original 2", (short) 2001, (short) 91);
		duplicate.setTmdbId(tmdbId);

		assertThatThrownBy(() -> movieRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class)
				.satisfies(exception -> assertThat(DuplicateMovieException.constraintOf(exception))
						.isEqualTo("uq_movie_tmdb_id"));
	}

	@Test
	void combinedFacetFiltersProduceEachMovieExactlyOnce() {
		Page<Movie> page = filtered("drama", List.of(), List.of(), "black and white", null,
				PageRequest.of(0, 1000));

		assertThat(page.getTotalElements()).isPositive();
		assertThat(page.getContent()).extracting(Movie::getId).doesNotHaveDuplicates();
		for (Movie movie : page.getContent()) {
			assertThat(movieHasGenre(movie.getId())).isTrue();
			assertThat(movieHasKeyword(movie.getId())).isTrue();
		}
	}

	@Test
	void personFiltersMatchTokensCaseInsensitivelyRegardlessOfOrder() {
		Page<Movie> directorBySurname = filtered(null, List.of(), List.of("\\mANDERSON\\M"),
				null, null, PageRequest.of(0, 100));
		assertThat(directorBySurname.getContent()).extracting(Movie::getTitle)
				.containsExactlyInAnyOrder("Rushmore", "The Royal Tenenbaums", "The Life Aquatic with Steve Zissou",
						"if....", "This Sporting Life", "Bottle Rocket", "The Darjeeling Limited", "Fantastic Mr. Fox",
						"Moonrise Kingdom", "Punch-Drunk Love", "Heart of a Dog", "King of Jazz",
						"The Grand Budapest Hotel", "Isle of Dogs",
						"The French Dispatch of the Liberty, Kansas Evening Sun");

		Page<Movie> directorSurnameFirst = filtered(null, List.of(), List.of("\\manderson\\M", "\\mwes\\M"),
				null, null, PageRequest.of(0, 100));
		assertThat(directorSurnameFirst.getContent()).extracting(Movie::getTitle)
				.containsExactlyInAnyOrder("Rushmore", "The Royal Tenenbaums", "The Life Aquatic with Steve Zissou",
						"Bottle Rocket", "The Darjeeling Limited", "Fantastic Mr. Fox", "Moonrise Kingdom",
						"The Grand Budapest Hotel", "Isle of Dogs",
						"The French Dispatch of the Liberty, Kansas Evening Sun");

		Page<Movie> actorBySurname = filtered(null, List.of("\\manderson\\M"), List.of(),
				null, null, PageRequest.of(0, 100));
		assertThat(actorBySurname.getContent()).extracting(Movie::getTitle)
				.containsExactlyInAnyOrder("Rebecca", "The Furies", "Hobson's Choice", "Paths of Glory", "Seconds",
						"Fantastic Mr. Fox", "Don't Play Us Cheap", "Buchanan Rides Alone",
						"Thirty Two Short Films About Glenn Gould");
	}

	@Test
	void personTokensMustMatchTheSamePerson() {
		Page<Movie> crossPersonTokens = filtered(null, List.of("\\mbill\\M", "\\manderson\\M"), List.of(),
				null, null, PageRequest.of(0, 100));

		assertThat(crossPersonTokens.getTotalElements()).isZero();
	}

	@Test
	void personTokensMatchWholeWordsNotSubstrings() {
		Page<Movie> wholeWord = filtered(null, List.of("\\mhill\\M"), List.of(),
				null, null, PageRequest.of(0, 100));
		assertThat(wholeWord.getContent()).extracting(Movie::getTitle)
				.containsExactlyInAnyOrder("The Leopard", "Medium Cool", "Dim Sum: A Little Bit of Heart");

		Page<Movie> substringOnly = filtered(null, List.of("\\mill\\M"), List.of(),
				null, null, PageRequest.of(0, 100));
		assertThat(substringOnly.getTotalElements()).isZero();
	}

	@Test
	void qMatchesWholeWordsNotSubstrings() {
		String suffix = String.valueOf(System.nanoTime());
		Movie killerProbe = movie("Killer " + suffix, "Killer Original " + suffix, (short) 2000, (short) 90);
		Movie illKidProbe = movie("The Ill Kid " + suffix, "The Ill Kid Original " + suffix,
				(short) 2001, (short) 91);
		movieRepository.saveAllAndFlush(List.of(killerProbe, illKidProbe));

		Page<Movie> matches = filtered(null, List.of(), List.of(), null,
				"\\mill\\M", PageRequest.of(0, 20));
		assertThat(matches.getContent()).extracting(Movie::getTitle)
				.containsExactly(illKidProbe.getTitle());
	}

	@Test
	void qRegexMetacharactersAreEscapedAsLiterals() {
		String suffix = String.valueOf(System.nanoTime());
		Movie dotProbe = movie("Dot.Probe " + suffix, "Dot.Probe Original " + suffix, (short) 2000, (short) 90);
		Movie dotXProbe = movie("DotXProbe " + suffix, "DotXProbe Original " + suffix, (short) 2001, (short) 91);
		movieRepository.saveAllAndFlush(List.of(dotProbe, dotXProbe));

		Page<Movie> literalDot = filtered(null, List.of(), List.of(), null,
				"\\mdot\\.probe\\M", PageRequest.of(0, 20));
		assertThat(literalDot.getContent()).extracting(Movie::getTitle)
				.containsExactly(dotProbe.getTitle());
	}

	@Test
	void junctionCardinalityAndPositionsMatchSeedLists() {
		Page<Movie> found = filtered(null, List.of(), List.of(), null,
				"\\mSeven Samurai\\M", PageRequest.of(0, 10));
		assertThat(found.getContent()).hasSize(1);
		Long id = found.getContent().getFirst().getId();

		List<MovieDirector> directors = movieDirectorRepository.findByMovieIdOrderByPosition(id);
		assertThat(directors).extracting(MovieDirector::getPosition).containsExactly((short) 1);
		List<MovieCast> cast = movieCastRepository.findByMovieIdOrderByPosition(id);
		assertThat(cast).hasSize(8);
		assertThat(cast).extracting(MovieCast::getPosition)
				.containsExactly((short) 1, (short) 2, (short) 3, (short) 4,
						(short) 5, (short) 6, (short) 7, (short) 8);
		List<MovieGenre> genres = movieGenreRepository.findByMovieIdOrderByPosition(id);
		assertThat(genres).hasSize(2);
		assertThat(genres).extracting(MovieGenre::getPosition).containsExactly((short) 1, (short) 2);
		List<MovieKeyword> keywords = movieKeywordRepository.findByMovieIdOrderByPosition(id);
		assertThat(keywords).hasSize(15);
		assertThat(keywords).extracting(MovieKeyword::getPosition)
				.containsExactly((short) 1, (short) 2, (short) 3, (short) 4, (short) 5,
						(short) 6, (short) 7, (short) 8, (short) 9, (short) 10,
						(short) 11, (short) 12, (short) 13, (short) 14, (short) 15);
	}

	@Test
	void duplicateDirectorInJunctionIsRejectedByConstraint() {
		Long movieId = probeMovieId();
		Person person = personRepository.findAllByNameIn(List.of("Toshirō Mifune")).getFirst();

		assertDuplicateJunctionRejected("uq_movie_director_person",
				directorRow(movieId, person.getId(), (short) 1),
				directorRow(movieId, person.getId(), (short) 2), movieDirectorRepository::saveAndFlush);
	}

	@Test
	void duplicateCastMemberInJunctionIsRejectedByConstraint() {
		Long movieId = probeMovieId();
		Person person = personRepository.findAllByNameIn(List.of("Toshirō Mifune")).getFirst();

		assertDuplicateJunctionRejected("uq_movie_cast_person",
				castRow(movieId, person.getId(), (short) 1),
				castRow(movieId, person.getId(), (short) 2), movieCastRepository::saveAndFlush);
	}

	@Test
	void duplicateGenreInJunctionIsRejectedByConstraint() {
		Long movieId = probeMovieId();
		Genre genre = genreRepository.findAllByNameIn(List.of("drama")).getFirst();

		assertDuplicateJunctionRejected("uq_movie_genre_genre",
				genreRow(movieId, genre.getId(), (short) 1),
				genreRow(movieId, genre.getId(), (short) 2), movieGenreRepository::saveAndFlush);
	}

	@Test
	void duplicateKeywordInJunctionIsRejectedByConstraint() {
		Long movieId = probeMovieId();
		Keyword keyword = keywordRepository.findAllByNameIn(List.of("samurai")).getFirst();

		assertDuplicateJunctionRejected("uq_movie_keyword_keyword",
				keywordRow(movieId, keyword.getId(), (short) 1),
				keywordRow(movieId, keyword.getId(), (short) 2), movieKeywordRepository::saveAndFlush);
	}

	@Test
	void movieDeleteCascadesToJunctions() {
		Movie probe = movie("Cascade Probe " + System.nanoTime(), "Cascade Original " + System.nanoTime(),
				(short) 2002, (short) 92);
		probe = movieRepository.saveAndFlush(probe);
		Genre drama = genreRepository.findAllByNameIn(List.of("drama")).getFirst();
		MovieGenre row = new MovieGenre();
		row.setMovieId(probe.getId());
		row.setGenreId(drama.getId());
		row.setPosition((short) 1);
		movieGenreRepository.saveAndFlush(row);

		movieRepository.deleteById(probe.getId());
		movieRepository.flush();

		assertThat(movieGenreRepository.findByMovieIdOrderByPosition(probe.getId())).isEmpty();
		assertThat(movieRepository.findById(probe.getId())).isEmpty();
	}

	@Test
	void seedUpsertIsIdempotentOnRerun() throws Exception {
		ClassPathResource seed = new ClassPathResource("db/migration/R__01_seed_movies.sql");
		String seedSql = seed.getContentAsString(StandardCharsets.UTF_8);

		jdbcTemplate.execute(seedSql);

		assertThat(movieRepository.count()).isEqualTo(1340L);
		Page<Movie> found = filtered(null, List.of(), List.of(), null,
				"\\mSeven Samurai\\M", PageRequest.of(0, 10));
		assertThat(found.getContent()).hasSize(1);
		assertThat(movieDirectorRepository.findByMovieIdOrderByPosition(found.getContent().getFirst().getId()))
				.hasSize(1);
	}

	private Page<Movie> filtered(String genre, List<String> actorTokens, List<String> directorTokens,
			String keyword, String qPattern, Pageable pageable) {
		return movieRepository.findFiltered(genre,
				token(actorTokens, 0), token(actorTokens, 1), token(actorTokens, 2), token(actorTokens, 3),
				token(actorTokens, 4),
				token(directorTokens, 0), token(directorTokens, 1), token(directorTokens, 2),
				token(directorTokens, 3), token(directorTokens, 4),
				keyword, null, qPattern, pageable);
	}

	private static String token(List<String> tokens, int index) {
		return index < tokens.size() ? tokens.get(index) : null;
	}

	private boolean movieHasGenre(Long movieId) {
		List<Integer> genreIds = movieGenreRepository.findByMovieIdOrderByPosition(movieId).stream()
				.map(MovieGenre::getGenreId)
				.toList();
		return genreRepository.findAllById(genreIds).stream()
				.anyMatch(genre -> genre.getName().equals("drama"));
	}

	private boolean movieHasKeyword(Long movieId) {
		List<Integer> keywordIds = movieKeywordRepository.findByMovieIdOrderByPosition(movieId).stream()
				.map(MovieKeyword::getKeywordId)
				.toList();
		return keywordRepository.findAllById(keywordIds).stream()
				.anyMatch(keyword -> keyword.getName().equals("black and white"));
	}

	private Long probeMovieId() {
		Movie probe = movie("Junction Dup Probe " + System.nanoTime(),
				"Junction Dup Original " + System.nanoTime(), (short) 2003, (short) 93);
		return movieRepository.saveAndFlush(probe).getId();
	}

	private <T> void assertDuplicateJunctionRejected(String constraint, T first, T second, Consumer<T> saver) {
		saver.accept(first);

		assertThatThrownBy(() -> saver.accept(second))
				.isInstanceOf(DataIntegrityViolationException.class)
				.satisfies(exception -> assertThat(DuplicateMovieException.constraintOf(exception))
						.isEqualTo(constraint));
	}

	private static MovieDirector directorRow(Long movieId, Integer personId, short position) {
		MovieDirector row = new MovieDirector();
		row.setMovieId(movieId);
		row.setPersonId(personId);
		row.setPosition(position);
		return row;
	}

	private static MovieCast castRow(Long movieId, Integer personId, short position) {
		MovieCast row = new MovieCast();
		row.setMovieId(movieId);
		row.setPersonId(personId);
		row.setPosition(position);
		return row;
	}

	private static MovieGenre genreRow(Long movieId, Integer genreId, short position) {
		MovieGenre row = new MovieGenre();
		row.setMovieId(movieId);
		row.setGenreId(genreId);
		row.setPosition(position);
		return row;
	}

	private static MovieKeyword keywordRow(Long movieId, Integer keywordId, short position) {
		MovieKeyword row = new MovieKeyword();
		row.setMovieId(movieId);
		row.setKeywordId(keywordId);
		row.setPosition(position);
		return row;
	}

	private static Movie movie(String title, String originalTitle, Short releaseYear, Short runtimeMinutes) {
		Movie movie = new Movie();
		movie.setTitle(title);
		movie.setOriginalTitle(originalTitle);
		movie.setReleaseYear(releaseYear == null ? (short) 2000 : releaseYear);
		movie.setRuntimeMinutes(runtimeMinutes == null ? (short) 90 : runtimeMinutes);
		movie.setTmdbId(uniqueTmdbId());
		return movie;
	}

	private static long uniqueTmdbId() {
		return TMDB_ID_SEQ.getAndIncrement();
	}

	private static final java.util.concurrent.atomic.AtomicLong TMDB_ID_SEQ =
			new java.util.concurrent.atomic.AtomicLong(900_000_000L);

}
