package com.movieplatform.recommendation.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

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
class TmdbClientTest {

	private static final String BASE_URL = "http://tmdb.test";

	private static final String TOKEN = "test-access-token";

	private static final String RECOMMENDATIONS_URI = BASE_URL + "/3/movie/550/recommendations?page=1";

	@Test
	void authenticatesWithBearerTokenAndParsesRecommendationResults() {
		RestClient.Builder builder = jsonBuilder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TmdbClient client = new TmdbClient(builder, BASE_URL, TOKEN);

		server.expect(requestTo(RECOMMENDATIONS_URI))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
				.andRespond(withSuccess("""
						{"page":1,"results":[
						  {"id":551,"title":"Fight Club","release_date":"1999-10-15",
						   "poster_path":"/fightclub.jpg","overview":"An insomniac office worker.",
						   "vote_average":8.4},
						  {"id":552,"title":"Memento","release_date":"2000-10-11",
						   "poster_path":null,"overview":null,"vote_average":8.2}
						]}
						""", MediaType.APPLICATION_JSON));

		List<TmdbMovie> recommendations = client.recommendations(550L);

		assertThat(recommendations).containsExactly(
				new TmdbMovie(551L, "Fight Club", "1999-10-15", "/fightclub.jpg",
						"An insomniac office worker.", 8.4),
				new TmdbMovie(552L, "Memento", "2000-10-11", null, null, 8.2));
	}

	@Test
	void rateLimitMapsToRateLimitedException() {
		RestClient.Builder builder = jsonBuilder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TmdbClient client = new TmdbClient(builder, BASE_URL, TOKEN);

		server.expect(requestTo(RECOMMENDATIONS_URI))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"status_message\":\"Too many requests\"}"));

		assertThatThrownBy(() -> client.recommendations(550L))
				.isInstanceOf(TmdbRateLimitedException.class);
	}

	@Test
	void ioFailureMapsToTimeoutException() {
		RestClient.Builder builder = RestClient.builder().requestFactory(failingFactory());
		TmdbClient client = new TmdbClient(builder, BASE_URL, TOKEN);

		assertThatThrownBy(() -> client.recommendations(550L))
				.isInstanceOf(TmdbTimeoutException.class);
	}

	@Test
	void unknownMovieYieldsNoRecommendationsInsteadOfFailing() {
		RestClient.Builder builder = jsonBuilder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TmdbClient client = new TmdbClient(builder, BASE_URL, TOKEN);

		server.expect(requestTo(RECOMMENDATIONS_URI))
				.andRespond(withStatus(HttpStatus.NOT_FOUND)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"status_message\":\"The resource you requested could not be found.\"}"));

		assertThat(client.recommendations(550L)).isEmpty();
	}

	@Test
	void serverErrorMapsToUpstreamException() {
		RestClient.Builder builder = jsonBuilder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		TmdbClient client = new TmdbClient(builder, BASE_URL, TOKEN);

		server.expect(requestTo(RECOMMENDATIONS_URI))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.recommendations(550L))
				.isInstanceOf(TmdbUpstreamException.class);
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
