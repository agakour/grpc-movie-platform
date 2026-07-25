package com.movieplatform.movie.util;

import org.springframework.stereotype.Component;

@Component
public class TmdbImageUrl {

	public static final String W342 = "w342";

	public static final String W500 = "w500";

	public static final String W1280 = "w1280";

	private static final String BASE_URL = "https://image.tmdb.org/t/p/";

	public String poster(String path) {
		return url(path, W342);
	}

	public String posterLarge(String path) {
		return url(path, W500);
	}

	public String backdrop(String path) {
		return url(path, W1280);
	}

	private String url(String path, String size) {
		if (path == null || path.isBlank()) {
			return null;
		}
		return BASE_URL + size + path;
	}

}
