package com.movieplatform.movie.remote.tmdb;

public class TmdbSearchException extends RuntimeException {

	public TmdbSearchException(String query, Throwable cause) {
		super("TMDb search failed for query '" + query + "'", cause);
	}

}
