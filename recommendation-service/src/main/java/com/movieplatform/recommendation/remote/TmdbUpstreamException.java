package com.movieplatform.recommendation.remote;

public class TmdbUpstreamException extends RuntimeException {

	public TmdbUpstreamException(long tmdbId, Throwable cause) {
		super("TMDb request failed while fetching recommendations for movie " + tmdbId, cause);
	}

}
