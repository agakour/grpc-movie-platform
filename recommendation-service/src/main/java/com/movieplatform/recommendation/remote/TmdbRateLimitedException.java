package com.movieplatform.recommendation.remote;

public class TmdbRateLimitedException extends RuntimeException {

	public TmdbRateLimitedException(long tmdbId, Throwable cause) {
		super("TMDb rate limit exceeded while fetching recommendations for movie " + tmdbId, cause);
	}

}
