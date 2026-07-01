package com.movieplatform.movie.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MovieListQuery(

		String genre,

		String actor,

		String director,

		String keyword,

		@Min(1888)
		@Max(2100)
		Integer year,

		String q,

		String sortBy,

		String dir,

		@Min(0)
		Integer page,

		@Min(1)
		@Max(100)
		Integer size) {

	public static final String SORT_TITLE = "title";
	public static final String SORT_RELEASE_YEAR = "releaseYear";
	public static final String SORT_RUNTIME_MINUTES = "runtimeMinutes";
	public static final String DIR_ASC = "asc";
	public static final String DIR_DESC = "desc";

	public MovieListQuery {
		sortBy = sortBy == null ? SORT_RELEASE_YEAR : sortBy;
		dir = dir == null ? DIR_DESC : dir;
		page = page == null ? 0 : page;
		size = size == null ? 20 : size;
	}

}
