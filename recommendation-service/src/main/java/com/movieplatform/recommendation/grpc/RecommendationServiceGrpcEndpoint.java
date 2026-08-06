package com.movieplatform.recommendation.grpc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import movieplatform.recommendation.Recommendation;
import movieplatform.recommendation.RecommendationRequest;
import movieplatform.recommendation.RecommendationServiceGrpc;

import com.movieplatform.recommendation.remote.TmdbClient;
import com.movieplatform.recommendation.remote.TmdbMovie;
import com.movieplatform.recommendation.remote.TmdbRateLimitedException;
import com.movieplatform.recommendation.remote.TmdbTimeoutException;
import com.movieplatform.recommendation.remote.TmdbUpstreamException;
import com.movieplatform.recommendation.service.RecommendationAggregator;
import com.movieplatform.recommendation.service.ScoredMovie;

@GrpcService
@SuppressWarnings("unused")
public class RecommendationServiceGrpcEndpoint extends RecommendationServiceGrpc.RecommendationServiceImplBase {

	private static final int DEFAULT_LIMIT = 20;

	private static final int MAX_CONCURRENT_LOOKUPS = 8;

	private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceGrpcEndpoint.class);

	private final TmdbClient tmdbClient;

	private final RecommendationAggregator aggregator;

	public RecommendationServiceGrpcEndpoint(TmdbClient tmdbClient) {
		this.tmdbClient = tmdbClient;
		this.aggregator = new RecommendationAggregator();
	}

	@Override
	public void getRecommendations(RecommendationRequest request,
			StreamObserver<Recommendation> responseObserver) {
		List<Long> likedTmdbIds = request.getTmdbIdsList().stream().distinct().toList();
		Map<Long, List<TmdbMovie>> recommendationsByLikedMovie = new LinkedHashMap<>();
		List<RuntimeException> failures = new ArrayList<>();
		try {
			lookUp(likedTmdbIds, recommendationsByLikedMovie, failures);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			fail(responseObserver, Status.UNAVAILABLE
					.withDescription("Recommendation lookup was interrupted")
					.withCause(ex));
			return;
		}
		if (recommendationsByLikedMovie.isEmpty() && !failures.isEmpty()) {
			fail(responseObserver, statusOf(failures));
			return;
		}
		if (!failures.isEmpty()) {
			logger.warn("Serving partial recommendations: {} of {} TMDb lookups failed",
					failures.size(), likedTmdbIds.size(), failures.getFirst());
		}

		int limit = request.getLimit() > 0 ? request.getLimit() : DEFAULT_LIMIT;
		for (ScoredMovie scored : aggregator.aggregate(likedTmdbIds, recommendationsByLikedMovie, limit)) {
			responseObserver.onNext(toProto(scored));
		}
		responseObserver.onCompleted();
	}

	private void lookUp(List<Long> likedTmdbIds, Map<Long, List<TmdbMovie>> recommendationsByLikedMovie,
			List<RuntimeException> failures) throws InterruptedException {
		Semaphore permits = new Semaphore(MAX_CONCURRENT_LOOKUPS);
		Map<Long, Future<List<TmdbMovie>>> pending = new LinkedHashMap<>();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (long tmdbId : likedTmdbIds) {
				pending.put(tmdbId, executor.submit(() -> {
					permits.acquire();
					try {
						return tmdbClient.recommendations(tmdbId);
					}
					finally {
						permits.release();
					}
				}));
			}
		}
		for (Map.Entry<Long, Future<List<TmdbMovie>>> entry : pending.entrySet()) {
			try {
				recommendationsByLikedMovie.put(entry.getKey(), entry.getValue().get());
			}
			catch (ExecutionException ex) {
				logger.warn("Skipping liked movie {}: TMDb lookup failed", entry.getKey(), ex.getCause());
				failures.add(ex.getCause() instanceof RuntimeException cause
						? cause : new TmdbUpstreamException(entry.getKey(), ex.getCause()));
			}
		}
	}

	private static Status statusOf(List<RuntimeException> failures) {
		for (RuntimeException failure : failures) {
			if (failure instanceof TmdbRateLimitedException) {
				return Status.RESOURCE_EXHAUSTED
						.withDescription("TMDb rate limit exceeded, try again later")
						.withCause(failure);
			}
		}
		for (RuntimeException failure : failures) {
			if (failure instanceof TmdbTimeoutException) {
				return Status.DEADLINE_EXCEEDED
						.withDescription("TMDb call timed out")
						.withCause(failure);
			}
		}
		RuntimeException first = failures.getFirst();
		if (first instanceof TmdbUpstreamException) {
			return Status.UNAVAILABLE
					.withDescription("TMDb is currently unavailable")
					.withCause(first);
		}
		return Status.INTERNAL
				.withDescription("Unexpected failure while building recommendations")
				.withCause(first);
	}

	private static void fail(StreamObserver<Recommendation> responseObserver, Status status) {
		responseObserver.onError(status.asRuntimeException());
	}

	private static Recommendation toProto(ScoredMovie scored) {
		TmdbMovie movie = scored.movie();
		return Recommendation.newBuilder()
				.setTmdbId(movie.id())
				.setTitle(movie.title() == null ? "" : movie.title())
				.setYear(releaseYear(movie.releaseDate()))
				.setPosterPath(movie.posterPath() == null ? "" : movie.posterPath())
				.setOverview(movie.overview() == null ? "" : movie.overview())
				.setScore(scored.score())
				.build();
	}

	private static int releaseYear(String releaseDate) {
		if (releaseDate == null || releaseDate.length() < 4) {
			return 0;
		}
		try {
			return Integer.parseInt(releaseDate.substring(0, 4));
		}
		catch (NumberFormatException ex) {
			return 0;
		}
	}

}
