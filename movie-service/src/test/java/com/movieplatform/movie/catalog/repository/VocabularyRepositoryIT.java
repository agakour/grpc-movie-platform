package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.Person;
import com.movieplatform.movie.catalog.entity.Genre;
import com.movieplatform.movie.catalog.entity.Keyword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.movieplatform.movie.exception.DuplicateMovieException;
import com.movieplatform.movie.testutil.PostgresTestSupport;

@Testcontainers
@DataJpaTest
@SuppressWarnings("unused")
class VocabularyRepositoryIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestSupport.POSTGRES_IMAGE);

	@Autowired
	private GenreRepository genreRepository;
	@Autowired
	private PersonRepository personRepository;
	@Autowired
	private KeywordRepository keywordRepository;

	@Test
	void findByNameInReturnsSeededLookupsInStableSet() {
		List<Genre> genres = genreRepository.findAllByNameIn(List.of("drama", "action"));

		assertThat(genres).extracting(Genre::getName).containsExactlyInAnyOrder("drama", "action");
	}

	@Test
	void duplicateGenreNameViolatesNameConstraint() {
		assertThatThrownBy(() -> genreRepository.saveAndFlush(new Genre("drama")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.satisfies(exception -> assertThat(DuplicateMovieException.constraintOf(exception))
						.isEqualTo("genre_name_key"));
	}

	@Test
	void duplicatePersonNameViolatesNameConstraint() {
		String existingName = personRepository.findAll(Sort.by("name")).getFirst().getName();

		assertThatThrownBy(() -> personRepository.saveAndFlush(new Person(existingName)))
				.isInstanceOf(DataIntegrityViolationException.class)
				.satisfies(exception -> assertThat(DuplicateMovieException.constraintOf(exception))
						.isEqualTo("person_name_key"));
	}

	@Test
	void duplicateKeywordNameViolatesNameConstraint() {
		String existingName = keywordRepository.findAll(Sort.by("name")).getFirst().getName();

		assertThatThrownBy(() -> keywordRepository.saveAndFlush(new Keyword(existingName)))
				.isInstanceOf(DataIntegrityViolationException.class)
				.satisfies(exception -> assertThat(DuplicateMovieException.constraintOf(exception))
						.isEqualTo("keyword_name_key"));
	}

}
