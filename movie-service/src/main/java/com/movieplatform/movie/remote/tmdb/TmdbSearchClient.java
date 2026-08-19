package com.movieplatform.movie.remote.tmdb;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@SuppressWarnings("unused")
public class TmdbSearchClient {

	private final RestClient restClient;

	public TmdbSearchClient(RestClient.Builder restClientBuilder,
			@Value("${tmdb.base-url:https://api.themoviedb.org}") String baseUrl,
			@Value("${tmdb.access-token:}") String accessToken) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.build();
	}

	public List<TmdbSearchResult> search(String query, Integer year) {
		try {
			TmdbSearchPage page = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/3/search/movie")
							.queryParam("query", query)
							.queryParam("year", year)
							.build())
					.retrieve()
					.body(TmdbSearchPage.class);
			return page == null || page.results() == null ? List.of() : page.results();
		}
		catch (RestClientException exception) {
			throw new TmdbSearchException(query, exception);
		}
	}

}
