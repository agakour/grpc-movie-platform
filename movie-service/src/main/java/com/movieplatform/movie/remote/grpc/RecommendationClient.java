package com.movieplatform.movie.remote.grpc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.grpc.StatusRuntimeException;

import movieplatform.recommendation.Recommendation;
import movieplatform.recommendation.RecommendationRequest;
import movieplatform.recommendation.RecommendationServiceGrpc;

@Component
@SuppressWarnings("unused")
public class RecommendationClient {

	static final String SERVICE = "recommendation-service";

	public static final int RESULTS_PER_MOVIE = 20;

	private static final long CALL_DEADLINE_SECONDS = 10;

	private final RecommendationServiceGrpc.RecommendationServiceBlockingStub stub;

	public RecommendationClient(RecommendationServiceGrpc.RecommendationServiceBlockingStub stub) {
		this.stub = stub;
	}

	public List<RecommendationItem> recommendations(List<Long> tmdbIds) {
		RecommendationRequest request = RecommendationRequest.newBuilder()
				.addAllTmdbIds(tmdbIds)
				.setLimit(RESULTS_PER_MOVIE * Math.max(1, tmdbIds.size()))
				.build();
		try {
			List<RecommendationItem> items = new ArrayList<>();
			Iterator<Recommendation> stream = stub.withDeadlineAfter(CALL_DEADLINE_SECONDS, TimeUnit.SECONDS)
					.getRecommendations(request);
			while (stream.hasNext()) {
				items.add(toItem(stream.next()));
			}
			return items;
		}
		catch (StatusRuntimeException exception) {
			throw new RemoteGrpcException(SERVICE, exception.getStatus().getCode(), exception);
		}
	}

	private static RecommendationItem toItem(Recommendation recommendation) {
		return new RecommendationItem(
				recommendation.getTmdbId(),
				recommendation.getTitle(),
				recommendation.getYear(),
				recommendation.getPosterPath(),
				recommendation.getOverview(),
				recommendation.getScore());
	}

}
