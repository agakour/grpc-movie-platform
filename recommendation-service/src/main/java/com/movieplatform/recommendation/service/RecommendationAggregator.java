package com.movieplatform.recommendation.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.movieplatform.recommendation.remote.TmdbMovie;

public class RecommendationAggregator {

	public List<ScoredMovie> aggregate(List<Long> likedTmdbIds,
			Map<Long, List<TmdbMovie>> recommendationsByLikedMovie, int limit) {
		Set<Long> liked = new HashSet<>(likedTmdbIds);
		Map<Long, Double> scores = new HashMap<>();
		Map<Long, TmdbMovie> moviesById = new HashMap<>();
		for (List<TmdbMovie> ranked : recommendationsByLikedMovie.values()) {
			for (int rank = 0; rank < ranked.size(); rank++) {
				TmdbMovie movie = ranked.get(rank);
				if (liked.contains(movie.id())) {
					continue;
				}
				scores.merge(movie.id(), 1.0 / (rank + 1), Double::sum);
				moviesById.putIfAbsent(movie.id(), movie);
			}
		}
		return scores.entrySet().stream()
				.sorted(Comparator.<Map.Entry<Long, Double>>comparingDouble(Map.Entry::getValue)
						.reversed()
						.thenComparing(Map.Entry::getKey))
				.limit(Math.max(limit, 0))
				.map(entry -> new ScoredMovie(moviesById.get(entry.getKey()), entry.getValue()))
				.toList();
	}

}
