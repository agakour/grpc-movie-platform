package com.movieplatform.movie.util;

import java.util.List;

public final class BrowseFilters {

	public static final List<String> GENRES = List.of(
			"action", "adventure", "comedy", "crime", "documentary", "drama",
			"family", "fantasy", "history", "horror", "music", "mystery",
			"romance", "science fiction", "thriller", "war");

	public static final List<String> KEYWORDS = List.of(
			"black and white", "based on novel or book", "france", "paris",
			"japan", "jidaigeki", "samurai", "murder", "film noir", "italy",
			"england", "love", "edo period", "world war ii",
			"based on play or musical", "woman director", "adultery", "germany",
			"small town", "suicide", "parent child relationship", "rural area",
			"love triangle", "prostitute", "london", "1970s", "satire",
			"dark comedy", "train", "surrealism", "coming of age", "police",
			"dreams", "feudal japan", "marriage", "california", "sibling relationship",
			"nazi", "neo-noir", "19th century", "tokyo");

	private BrowseFilters() {
	}

}
