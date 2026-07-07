package com.movieplatform.movie.security.registration;

public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("Username already in use: '" + username + "'");
	}

}
