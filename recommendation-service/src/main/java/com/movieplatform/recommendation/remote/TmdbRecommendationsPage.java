package com.movieplatform.recommendation.remote;

import java.util.List;

public record TmdbRecommendationsPage(List<TmdbMovie> results) {
}
