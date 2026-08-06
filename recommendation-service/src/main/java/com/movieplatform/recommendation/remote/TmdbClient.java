package com.movieplatform.recommendation.remote;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@SuppressWarnings("unused")
public class TmdbClient {

	private static final Logger logger = LoggerFactory.getLogger(TmdbClient.class);

	private final RestClient restClient;

	public TmdbClient(RestClient.Builder restClientBuilder,
			@Value("${tmdb.base-url:https://api.themoviedb.org}") String baseUrl,
			@Value("${tmdb.access-token:}") String accessToken) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.build();
	}

	public List<TmdbMovie> recommendations(long tmdbId) {
		try {
			TmdbRecommendationsPage page = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/3/movie/{id}/recommendations")
							.queryParam("page", 1)
							.build(tmdbId))
					.retrieve()
					.body(TmdbRecommendationsPage.class);
			return page == null || page.results() == null ? List.of() : page.results();
		}
		catch (HttpClientErrorException.NotFound ex) {
			logger.warn("TMDb does not know movie {} — skipping it while building recommendations", tmdbId);
			return List.of();
		}
		catch (HttpClientErrorException.TooManyRequests ex) {
			throw new TmdbRateLimitedException(tmdbId, ex);
		}
		catch (ResourceAccessException ex) {
			throw new TmdbTimeoutException(tmdbId, ex);
		}
		catch (RestClientException ex) {
			throw new TmdbUpstreamException(tmdbId, ex);
		}
	}

}
