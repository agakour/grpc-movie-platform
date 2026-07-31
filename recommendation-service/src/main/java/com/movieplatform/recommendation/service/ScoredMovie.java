package com.movieplatform.recommendation.service;

import com.movieplatform.recommendation.remote.TmdbMovie;

public record ScoredMovie(TmdbMovie movie, double score) {
}
