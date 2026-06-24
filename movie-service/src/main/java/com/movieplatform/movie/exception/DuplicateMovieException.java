package com.movieplatform.movie.exception;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuplicateMovieException extends RuntimeException {

	private static final Pattern CONSTRAINT_PATTERN =
			Pattern.compile("unique constraint \"([^\"]+)\"");

	private final String constraintName;

	public DuplicateMovieException(String constraintName) {
		super("Duplicate value violates constraint: " + constraintName);
		this.constraintName = constraintName;
	}

	public String getConstraintName() {
		return constraintName;
	}

	public static String constraintOf(Throwable throwable) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			String message = current.getMessage() == null ? "" : current.getMessage();
			Matcher matcher = CONSTRAINT_PATTERN.matcher(message);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		return null;
	}

}
