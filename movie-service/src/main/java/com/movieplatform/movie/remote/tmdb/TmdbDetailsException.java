package com.movieplatform.movie.remote.tmdb;

public class TmdbDetailsException extends RuntimeException {

	public TmdbDetailsException(long tmdbId, Throwable cause) {
		super("TMDb movie details failed for id " + tmdbId, cause);
	}

}
