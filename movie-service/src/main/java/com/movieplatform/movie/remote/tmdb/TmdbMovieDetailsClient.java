package com.movieplatform.movie.remote.tmdb;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@SuppressWarnings("unused")
public class TmdbMovieDetailsClient {

	private static final int CAST_LIMIT = 8;

	private final RestClient restClient;

	public TmdbMovieDetailsClient(RestClient.Builder restClientBuilder,
			@Value("${tmdb.base-url:https://api.themoviedb.org}") String baseUrl,
			@Value("${tmdb.access-token:}") String accessToken) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.build();
	}

	public TmdbMovieEnrichment enrich(long tmdbId) {
		try {
			TmdbMovieDetails details = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/3/movie/{id}").build(tmdbId))
					.retrieve()
					.body(TmdbMovieDetails.class);
			if (details == null) {
				throw new TmdbDetailsException(tmdbId,
						new IllegalStateException("TMDb returned an empty movie-details body"));
			}
			TmdbCredits credits = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/3/movie/{id}/credits").build(tmdbId))
					.retrieve()
					.body(TmdbCredits.class);
			TmdbKeywordsPage keywords = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/3/movie/{id}/keywords").build(tmdbId))
					.retrieve()
					.body(TmdbKeywordsPage.class);
			return new TmdbMovieEnrichment(
					tmdbId,
					details.title(),
					details.originalTitle(),
					details.overview(),
					releaseYear(details.releaseDate()),
					details.runtime(),
					details.originalLanguage(),
					details.posterPath(),
					details.backdropPath(),
					directors(credits),
					cast(credits),
					genres(details),
					keywordNames(keywords));
		}
		catch (RestClientException exception) {
			throw new TmdbDetailsException(tmdbId, exception);
		}
	}

	private static List<String> directors(TmdbCredits credits) {
		if (credits == null || credits.crew() == null) {
			return List.of();
		}
		return credits.crew().stream()
				.filter(crew -> "Director".equals(crew.job()))
				.map(TmdbCredits.CrewMember::name)
				.toList();
	}

	private static List<String> cast(TmdbCredits credits) {
		if (credits == null || credits.cast() == null) {
			return List.of();
		}
		return credits.cast().stream()
				.sorted(Comparator.comparing(
						TmdbCredits.CastMember::order,
						Comparator.nullsLast(Comparator.naturalOrder())))
				.limit(CAST_LIMIT)
				.map(TmdbCredits.CastMember::name)
				.toList();
	}

	private static List<String> genres(TmdbMovieDetails details) {
		if (details.genres() == null) {
			return List.of();
		}
		return details.genres().stream()
				.map(TmdbMovieDetails.Genre::name)
				.map(name -> name.toLowerCase(Locale.ROOT))
				.toList();
	}

	private static List<String> keywordNames(TmdbKeywordsPage keywords) {
		if (keywords == null || keywords.keywords() == null) {
			return List.of();
		}
		return keywords.keywords().stream()
				.map(TmdbKeywordsPage.Keyword::name)
				.map(name -> name.toLowerCase(Locale.ROOT))
				.toList();
	}

	private static Integer releaseYear(String releaseDate) {

		if (releaseDate == null || releaseDate.length() < 4) {
			return null;
		}
		try {
			return Integer.parseInt(releaseDate.substring(0, 4));
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

}
