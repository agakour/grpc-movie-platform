package com.movieplatform.movie.remote.tmdb;

import java.util.List;

public record TmdbKeywordsPage(long id, List<Keyword> keywords) {

	@SuppressWarnings("unused")
	public record Keyword(long id, String name) {
	}

}
