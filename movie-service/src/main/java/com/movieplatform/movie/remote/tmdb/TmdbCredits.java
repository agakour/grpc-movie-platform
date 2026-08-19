package com.movieplatform.movie.remote.tmdb;

import java.util.List;

public record TmdbCredits(List<CastMember> cast, List<CrewMember> crew) {

	@SuppressWarnings("unused")
	public record CastMember(long id, String name, Integer order) {
	}

	@SuppressWarnings("unused")
	public record CrewMember(long id, String name, String job, String department) {
	}

}
