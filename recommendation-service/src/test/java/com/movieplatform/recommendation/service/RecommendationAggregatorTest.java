package com.movieplatform.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.movieplatform.recommendation.remote.TmdbMovie;

@SuppressWarnings("unused")
class RecommendationAggregatorTest {

	private final RecommendationAggregator aggregator = new RecommendationAggregator();

	@Test
	void sumsReciprocalRankAcrossLikedMovies() {
		Map<Long, List<TmdbMovie>> lists = Map.of(
				1L, List.of(movie(10L), movie(20L)),
				2L, List.of(movie(20L), movie(30L), movie(10L)));

		List<ScoredMovie> result = aggregator.aggregate(List.of(1L, 2L), lists, 10);

		assertThat(result).extracting(ScoredMovie::movie).extracting(TmdbMovie::id)
				.containsExactly(20L, 10L, 30L);
		assertThat(result).extracting(ScoredMovie::score)
				.containsExactly(1.5, 4.0 / 3.0, 0.5);
	}

	@Test
	void excludesLikedMovieIds() {
		Map<Long, List<TmdbMovie>> lists = Map.of(
				1L, List.of(movie(2L), movie(10L)));

		List<ScoredMovie> result = aggregator.aggregate(List.of(1L, 2L), lists, 10);

		assertThat(result).extracting(ScoredMovie::movie).extracting(TmdbMovie::id)
				.containsExactly(10L);
	}

	@Test
	void capsAtLimit() {
		Map<Long, List<TmdbMovie>> lists = Map.of(
				1L, List.of(movie(10L), movie(20L), movie(30L)));

		List<ScoredMovie> result = aggregator.aggregate(List.of(1L), lists, 2);

		assertThat(result).extracting(ScoredMovie::movie).extracting(TmdbMovie::id)
				.containsExactly(10L, 20L);
	}

	@Test
	void tiesBreakByAscendingTmdbId() {
		Map<Long, List<TmdbMovie>> lists = Map.of(
				1L, List.of(movie(20L), movie(10L)),
				2L, List.of(movie(10L), movie(20L)));

		List<ScoredMovie> result = aggregator.aggregate(List.of(1L, 2L), lists, 10);

		assertThat(result).extracting(ScoredMovie::movie).extracting(TmdbMovie::id)
				.containsExactly(10L, 20L);
	}

	@Test
	void emptyInputYieldsEmptyResult() {
		assertThat(aggregator.aggregate(List.of(), Map.of(), 20)).isEmpty();
	}

	@Test
	void nonPositiveLimitYieldsEmptyResult() {
		Map<Long, List<TmdbMovie>> lists = Map.of(1L, List.of(movie(10L)));

		assertThat(aggregator.aggregate(List.of(1L), lists, 0)).isEmpty();
	}

	private static TmdbMovie movie(long id) {
		return new TmdbMovie(id, "Movie " + id, null, null, null, 0.0);
	}

}
