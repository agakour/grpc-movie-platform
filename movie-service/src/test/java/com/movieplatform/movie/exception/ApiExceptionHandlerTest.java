package com.movieplatform.movie.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@SuppressWarnings("unused")
class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void uqMovieTmdbIdConstraintMapsToDuplicateMovieProblem() {
		ProblemDetail problem = handler.handleDuplicate(new DuplicateMovieException("uq_movie_tmdb_id"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:duplicate-movie"));
		assertThat(problem.getTitle()).isEqualTo("Duplicate movie");
	}

	@Test
	void vocabularyNameConstraintMapsToDuplicateNameProblem() {
		ProblemDetail problem = handler.handleDuplicate(new DuplicateMovieException("genre_name_key"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:duplicate-name"));
		assertThat(problem.getTitle()).isEqualTo("Duplicate name");
		assertThat(problem.getDetail()).contains("genre");
	}

	@Test
	void personNameConstraintMapsToDuplicateNameProblem() {
		ProblemDetail problem = handler.handleDuplicate(new DuplicateMovieException("person_name_key"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:duplicate-name"));
		assertThat(problem.getDetail()).contains("person");
	}

	@Test
	void unknownConstraintMapsToGenericConflict() {
		ProblemDetail problem = handler.handleDuplicate(new DuplicateMovieException("movie_pkey"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:duplicate-name"));
	}

	@Test
	void notFoundMapsToProblemWithResourceAndId() {
		ProblemDetail problem = handler.handleNotFound(new NotFoundException("movie", 42L));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(problem.getType()).isEqualTo(URI.create("urn:problem:not-found"));
		assertThat(problem.getProperties()).containsEntry("resource", "movie").containsEntry("id", 42L);
	}

}
