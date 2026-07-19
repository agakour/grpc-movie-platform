package com.movieplatform.movie.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = RestController.class)
@SuppressWarnings("unused")
public class ApiExceptionHandler {

	static final URI TYPE_VALIDATION = URI.create("urn:problem:validation-error");
	static final URI TYPE_INVALID_REQUEST = URI.create("urn:problem:invalid-request");
	static final URI TYPE_NOT_FOUND = URI.create("urn:problem:not-found");
	static final URI TYPE_DUPLICATE_MOVIE = URI.create("urn:problem:duplicate-movie");
	static final URI TYPE_DUPLICATE_NAME = URI.create("urn:problem:duplicate-name");

	@ExceptionHandler(NotFoundException.class)
	public ProblemDetail handleNotFound(NotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				exception.getResource() + " " + exception.getId() + " not found");
		problem.setType(TYPE_NOT_FOUND);
		problem.setTitle("Not found");
		problem.setProperty("resource", exception.getResource());
		problem.setProperty("id", exception.getId());
		return problem;
	}

	@ExceptionHandler(DuplicateMovieException.class)
	public ProblemDetail handleDuplicate(DuplicateMovieException exception) {
		String constraint = exception.getConstraintName() == null ? "" : exception.getConstraintName();
		if (constraint.contains("uq_movie_tmdb_id")) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
					"A movie with this tmdb_id already exists.");
			problem.setType(TYPE_DUPLICATE_MOVIE);
			problem.setTitle("Duplicate movie");
			return problem;
		}
		if (constraint.endsWith("_name_key")) {
			String kind = constraint.substring(0, constraint.length() - "_name_key".length());
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
					"A " + kind + " with this name already exists.");
			problem.setType(TYPE_DUPLICATE_NAME);
			problem.setTitle("Duplicate name");
			return problem;
		}
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Duplicate value violates constraint " + constraint + ".");
		problem.setType(TYPE_DUPLICATE_NAME);
		problem.setTitle("Duplicate value");
		return problem;
	}

	@ExceptionHandler(BindException.class)
	public ProblemDetail handleBind(BindException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Request validation failed.");
		problem.setType(TYPE_VALIDATION);
		problem.setTitle("Validation failed");
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(fieldError.getField(),
					fieldError.getDefaultMessage() == null ? "invalid value" : fieldError.getDefaultMessage());
		}
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Parameter validation failed.");
		problem.setType(TYPE_VALIDATION);
		problem.setTitle("Validation failed");
		Map<String, String> errors = new LinkedHashMap<>();
		for (ParameterValidationResult result : exception.getParameterValidationResults()) {
			String parameterName = result.getMethodParameter().getParameterName();
			String message = result.getResolvableErrors().stream()
					.map(MessageSourceResolvable::getDefaultMessage)
					.filter(Objects::nonNull)
					.collect(Collectors.joining("; "));
			errors.put(parameterName == null ? "parameter" : parameterName, message);
		}
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Parameter '" + exception.getName() + "' has invalid value '" + exception.getValue() + "'.");
		problem.setType(TYPE_INVALID_REQUEST);
		problem.setTitle("Invalid parameter");
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleNotReadable(HttpMessageNotReadableException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Request body is malformed or unreadable.");
		problem.setType(TYPE_INVALID_REQUEST);
		problem.setTitle("Invalid request body");
		return problem;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				exception.getMessage() == null ? "Invalid argument." : exception.getMessage());
		problem.setType(TYPE_INVALID_REQUEST);
		problem.setTitle("Invalid argument");
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
		String constraint = DuplicateMovieException.constraintOf(exception);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				constraint == null
						? "The write violates a database constraint."
						: "The write violates database constraint " + constraint + ".");
		problem.setType(TYPE_DUPLICATE_NAME);
		problem.setTitle("Constraint violation");
		return problem;
	}

}
