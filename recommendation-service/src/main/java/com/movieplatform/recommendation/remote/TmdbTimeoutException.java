package com.movieplatform.recommendation.remote;

public class TmdbTimeoutException extends RuntimeException {

	public TmdbTimeoutException(long tmdbId, Throwable cause) {
		super("TMDb call timed out while fetching recommendations for movie " + tmdbId, cause);
	}

}
