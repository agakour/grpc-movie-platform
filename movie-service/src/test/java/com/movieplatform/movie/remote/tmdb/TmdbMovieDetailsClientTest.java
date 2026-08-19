package com.movieplatform.movie.remote.tmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@SuppressWarnings("unused")
class TmdbMovieDetailsClientTest {

	private static final String BASE_URL = "http://tmdb.test";

	private static final String TOKEN = "test-access-token";

	private static final String DETAILS_URI = BASE_URL + "/3/movie/27205";

	private static final String CREDITS_URI = BASE_URL + "/3/movie/27205/credits";

	private static final String KEYWORDS_URI = BASE_URL + "/3/movie/27205/keywords";

	@Test
	void authenticatesWithBearerTokenAndMapsDetailsCreditsAndKeywords() {
		RestClient.Builder builder = jsonBuilder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TmdbMovieDetailsClient client = new TmdbMovieDetailsClient(builder, BASE_URL, TOKEN);

		server.expect(requestTo(DETAILS_URI))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
				.andRespond(withSuccess("""
						{"id":27205,"title":"Inception","original_title":"Inception",
						 "release_date":"2010-07-15","runtime":148,
						 "poster_path":"/p.jpg","backdrop_path":"/b.jpg",
						 "overview":"A thief who steals corporate secrets.","original_language":"en",
						 "genres":[{"id":28,"name":"Action"},{"id":878,"name":"Science Fiction"}]}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(CREDITS_URI))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
				.andRespond(withSuccess("""
						{"cast":[
						  {"id":8,"name":"Tom Berenger","order":7},
						  {"id":3,"name":"Elliot Page","order":2},
						  {"id":1,"name":"Leonardo DiCaprio","order":0},
						  {"id":6,"name":"Dileep Rao","order":5},
						  {"id":9,"name":"Michael Caine","order":8},
						  {"id":2,"name":"Joseph Gordon-Levitt","order":1},
						  {"id":4,"name":"Tom Hardy","order":3},
						  {"id":7,"name":"Cillian Murphy","order":6},
						  {"id":5,"name":"Ken Watanabe","order":4}
						],
						 "crew":[
						  {"id":10,"name":"Christopher Nolan","job":"Director","department":"Directing"},
						  {"id":11,"name":"Emma Thomas","job":"Producer","department":"Production"}
						]}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(KEYWORDS_URI))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
				.andRespond(withSuccess("""
						{"id":27205,"keywords":[{"id":1,"name":"Dream"},{"id":2,"name":"Heist"}]}
						""", MediaType.APPLICATION_JSON));

		TmdbMovieEnrichment enrichment = client.enrich(27205L);

		assertThat(enrichment.tmdbId()).isEqualTo(27205L);
		assertThat(enrichment.title()).isEqualTo("Inception");
		assertThat(enrichment.originalTitle()).isEqualTo("Inception");
		assertThat(enrichment.overview()).isEqualTo("A thief who steals corporate secrets.");
		assertThat(enrichment.releaseYear()).isEqualTo(2010);
		assertThat(enrichment.runtimeMinutes()).isEqualTo(148);
		assertThat(enrichment.originalLanguage()).isEqualTo("en");
		assertThat(enrichment.posterPath()).isEqualTo("/p.jpg");
		assertThat(enrichment.backdropPath()).isEqualTo("/b.jpg");
		assertThat(enrichment.directors()).containsExactly("Christopher Nolan");
		assertThat(enrichment.cast()).containsExactly(
				"Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page", "Tom Hardy",
				"Ken Watanabe", "Dileep Rao", "Cillian Murphy", "Tom Berenger");
		assertThat(enrichment.genres()).containsExactly("action", "science fiction");
		assertThat(enrichment.keywords()).containsExactly("dream", "heist");
	}

	@Test
	void rateLimitMapsToDetailsException() {
		RestClient.Builder builder = jsonBuilder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TmdbMovieDetailsClient client = new TmdbMovieDetailsClient(builder, BASE_URL, TOKEN);

		server.expect(requestTo(DETAILS_URI))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"status_message\":\"Too many requests\"}"));

		assertThatThrownBy(() -> client.enrich(27205L))
				.isInstanceOf(TmdbDetailsException.class);
	}

	@Test
	void networkErrorMapsToDetailsException() {
		RestClient.Builder builder = RestClient.builder().requestFactory(failingFactory());
		TmdbMovieDetailsClient client = new TmdbMovieDetailsClient(builder, BASE_URL, TOKEN);

		assertThatThrownBy(() -> client.enrich(27205L))
				.isInstanceOf(TmdbDetailsException.class);
	}

	private static RestClient.Builder jsonBuilder() {
		return RestClient.builder()
				.configureMessageConverters(builder -> builder.withJsonConverter(new JacksonJsonHttpMessageConverter()));
	}

	private static ClientHttpRequestFactory failingFactory() {
		return (uri, method) -> {
			throw new ResourceAccessException("connect timed out");
		};
	}

}
