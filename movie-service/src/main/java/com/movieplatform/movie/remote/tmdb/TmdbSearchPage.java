package com.movieplatform.movie.remote.tmdb;

import java.util.List;

public record TmdbSearchPage(List<TmdbSearchResult> results) {
}
