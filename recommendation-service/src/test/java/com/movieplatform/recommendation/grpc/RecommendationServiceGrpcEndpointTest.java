package com.movieplatform.recommendation.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import movieplatform.recommendation.Recommendation;
import movieplatform.recommendation.RecommendationRequest;

import com.movieplatform.recommendation.remote.TmdbClient;
import com.movieplatform.recommendation.remote.TmdbMovie;
import com.movieplatform.recommendation.remote.TmdbRateLimitedException;
import com.movieplatform.recommendation.remote.TmdbTimeoutException;
import com.movieplatform.recommendation.remote.TmdbUpstreamException;

@SuppressWarnings("unused")
class RecommendationServiceGrpcEndpointTest {

	@Test
	void streamsAggregatedRecommendationsRankedByScore() {
		FakeTmdbClient tmdbClient = new FakeTmdbClient(Map.of(
				550L, List.of(movie(551L, "Fight Club", "1999-10-15"),
						movie(552L, "Memento", "2000-10-11")),
				680L, List.of(movie(552L, "Memento", "2000-10-11"))));
		RecommendationServiceGrpcEndpoint endpoint = new RecommendationServiceGrpcEndpoint(tmdbClient);
		RecordingObserver observer = new RecordingObserver();

		endpoint.getRecommendations(RecommendationRequest.newBuilder()
				.addAllTmdbIds(List.of(550L, 680L))
				.setLimit(10)
				.build(), observer);

		assertThat(observer.completed).isTrue();
		assertThat(observer.error).isNull();

		assertThat(observer.items).extracting(Recommendation::getTmdbId)
				.containsExactly(552L, 551L);
		assertThat(observer.items).extracting(Recommendation::getScore)
				.containsExactly(1.5, 1.0);
		assertThat(observer.items.getFirst().getTitle()).isEqualTo("Memento");
		assertThat(observer.items.getFirst().getYear()).isEqualTo(2000);
		assertThat(observer.items.getFirst().getPosterPath()).isEmpty();
	}

	@Test
	void emptyRequestCompletesWithoutItems() {
		RecommendationServiceGrpcEndpoint endpoint = new RecommendationServiceGrpcEndpoint(
				new FakeTmdbClient(Map.of()));
		RecordingObserver observer = new RecordingObserver();

		endpoint.getRecommendations(RecommendationRequest.getDefaultInstance(), observer);

		assertThat(observer.completed).isTrue();
		assertThat(observer.items).isEmpty();
	}

	@Test
	void failingLikedMovieIsSkippedInsteadOfEmptyingTheResult() {
		FakeTmdbClient tmdbClient = new FakeTmdbClient(
				Map.of(550L, List.of(movie(551L, "Fight Club", "1999-10-15"))),
				Map.of(999L, new TmdbUpstreamException(999L, null)));
		RecommendationServiceGrpcEndpoint endpoint = new RecommendationServiceGrpcEndpoint(tmdbClient);
		RecordingObserver observer = new RecordingObserver();

		endpoint.getRecommendations(RecommendationRequest.newBuilder()
				.addAllTmdbIds(List.of(550L, 999L))
				.setLimit(10)
				.build(), observer);

		assertThat(observer.completed).isTrue();
		assertThat(observer.error).isNull();
		assertThat(observer.items).extracting(Recommendation::getTmdbId).containsExactly(551L);
	}

	@Test
	void rateLimitFailureSurfacesResourceExhausted() {
		FakeTmdbClient tmdbClient = new FakeTmdbClient(Map.of());
		tmdbClient.failWith(new TmdbRateLimitedException(550L, null));

		assertFailsWithCode(new RecommendationServiceGrpcEndpoint(tmdbClient), Status.Code.RESOURCE_EXHAUSTED);
	}

	@Test
	void timeoutFailureSurfacesDeadlineExceeded() {
		FakeTmdbClient tmdbClient = new FakeTmdbClient(Map.of());
		tmdbClient.failWith(new TmdbTimeoutException(550L, null));

		assertFailsWithCode(new RecommendationServiceGrpcEndpoint(tmdbClient), Status.Code.DEADLINE_EXCEEDED);
	}

	@Test
	void upstreamFailureSurfacesUnavailable() {
		FakeTmdbClient tmdbClient = new FakeTmdbClient(Map.of());
		tmdbClient.failWith(new TmdbUpstreamException(550L, null));

		assertFailsWithCode(new RecommendationServiceGrpcEndpoint(tmdbClient), Status.Code.UNAVAILABLE);
	}

	private static void assertFailsWithCode(RecommendationServiceGrpcEndpoint endpoint, Status.Code code) {
		RecordingObserver observer = new RecordingObserver();

		endpoint.getRecommendations(RecommendationRequest.newBuilder()
				.addTmdbIds(550L)
				.build(), observer);

		assertThat(observer.completed).isFalse();
		assertThat(observer.error).isNotNull();
		assertThat(Status.fromThrowable(observer.error).getCode()).isEqualTo(code);
	}

	private static TmdbMovie movie(long id, String title, String releaseDate) {
		return new TmdbMovie(id, title, releaseDate, null, null, 0.0);
	}

	private static final class FakeTmdbClient extends TmdbClient {

		private final Map<Long, List<TmdbMovie>> responses;

		private final Map<Long, RuntimeException> movieFailures;

		private RuntimeException failure;

		FakeTmdbClient(Map<Long, List<TmdbMovie>> responses) {
			this(responses, Map.of());
		}

		FakeTmdbClient(Map<Long, List<TmdbMovie>> responses, Map<Long, RuntimeException> movieFailures) {
			super(RestClient.builder(), "http://unused", "unused-key");
			this.responses = responses;
			this.movieFailures = movieFailures;
		}

		@Override
		public List<TmdbMovie> recommendations(long tmdbId) {
			if (failure != null) {
				throw failure;
			}
			RuntimeException movieFailure = movieFailures.get(tmdbId);
			if (movieFailure != null) {
				throw movieFailure;
			}
			return responses.getOrDefault(tmdbId, List.of());
		}

		void failWith(RuntimeException failure) {
			this.failure = failure;
		}

	}

	private static final class RecordingObserver implements StreamObserver<Recommendation> {

		private final List<Recommendation> items = new ArrayList<>();

		private boolean completed;

		private Throwable error;

		@Override
		public void onNext(Recommendation value) {
			items.add(value);
		}

		@Override
		public void onError(Throwable t) {
			this.error = t;
		}

		@Override
		public void onCompleted() {
			this.completed = true;
		}

	}

}
