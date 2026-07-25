package com.movieplatform.movie.web;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.movieplatform.movie.catalog.dto.MovieListQuery;
import com.movieplatform.movie.catalog.dto.PageResponse;
import com.movieplatform.movie.catalog.dto.MovieSummaryResponse;
import com.movieplatform.movie.catalog.service.MovieService;
import com.movieplatform.movie.remote.grpc.RemoteGrpcException;
import com.movieplatform.movie.remote.grpc.VoteClient;
import com.movieplatform.movie.remote.grpc.VoteItem;
import com.movieplatform.movie.security.session.CurrentUser;
import com.movieplatform.movie.security.session.MoviePlatformUser;
import com.movieplatform.movie.util.BrowseFilters;
import com.movieplatform.movie.util.TmdbImageUrl;

@Controller
@SuppressWarnings("unused")
public class BrowseWebController {

	private static final int DEFAULT_SIZE = 20;

	private static final Logger logger = LoggerFactory.getLogger(BrowseWebController.class);

	private final MovieService movieService;

	private final VoteClient voteClient;

	private final CurrentUser currentUser;

	private final TmdbImageUrl tmdbImageUrl;

	public BrowseWebController(MovieService movieService, VoteClient voteClient, CurrentUser currentUser,
			TmdbImageUrl tmdbImageUrl) {
		this.movieService = movieService;
		this.voteClient = voteClient;
		this.currentUser = currentUser;
		this.tmdbImageUrl = tmdbImageUrl;
	}

	@GetMapping("/")
	public String browse(
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "genre", required = false) String genre,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "director", required = false) String director,
			@RequestParam(name = "actor", required = false) String actor,
			@RequestParam(name = "year", required = false) Integer year,
			@RequestParam(name = "sortBy", required = false) String sortBy,
			@RequestParam(name = "dir", required = false) String dir,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "size", required = false) Integer size,
			Model model) {
		int safePage = page == null || page < 0 ? 0 : page;
		int safeSize = size == null ? DEFAULT_SIZE : Math.clamp(size, 1, 100);
		String safeSort = switch (sortBy == null ? "" : sortBy) {
			case MovieListQuery.SORT_TITLE, MovieListQuery.SORT_RELEASE_YEAR,
					MovieListQuery.SORT_RUNTIME_MINUTES -> sortBy;
			default -> MovieListQuery.SORT_RELEASE_YEAR;
		};
		String safeDir = (MovieListQuery.DIR_ASC.equals(dir) || MovieListQuery.DIR_DESC.equals(dir))
				? dir : MovieListQuery.DIR_DESC;
		String safeGenre = genre != null && BrowseFilters.GENRES.contains(genre) ? genre : null;
		String safeKeyword = keyword != null && BrowseFilters.KEYWORDS.contains(keyword) ? keyword : null;

		MovieListQuery query = new MovieListQuery(safeGenre, blankToNull(actor), blankToNull(director),
				safeKeyword, year, blankToNull(q), safeSort, safeDir, safePage, safeSize);
		PageResponse<MovieSummaryResponse> result = movieService.list(query);
		if (result.totalPages() > 0 && safePage >= result.totalPages()) {
			return "redirect:" + pageUrl(query, result.totalPages() - 1);
		}

		model.addAttribute("movies", result);
		model.addAttribute("cards", result.content().stream()
				.map(movie -> new MovieCardView(
						movie.id(),
						movie.title(),
						movie.releaseYear(),
						movie.runtimeMinutes(),
						tmdbImageUrl.poster(movie.posterPath())))
				.toList());
		model.addAttribute("genres", BrowseFilters.GENRES);
		model.addAttribute("keywords", BrowseFilters.KEYWORDS);
		model.addAttribute("query", query);
		model.addAttribute("prevPageUrl", result.page() > 0 ? pageUrl(query, safePage - 1) : null);
		model.addAttribute("nextPageUrl",
				result.page() + 1 < result.totalPages() ? pageUrl(query, safePage + 1) : null);
		VoteBadges badges = voteBadges();
		model.addAttribute("likedMovieIds", badges.liked());
		model.addAttribute("dislikedMovieIds", badges.disliked());
		return "browse";
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String pageUrl(MovieListQuery query, int page) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
		if (query.q() != null) {
			builder.queryParam("q", query.q());
		}
		if (query.genre() != null) {
			builder.queryParam("genre", query.genre());
		}
		if (query.keyword() != null) {
			builder.queryParam("keyword", query.keyword());
		}
		if (query.director() != null) {
			builder.queryParam("director", query.director());
		}
		if (query.actor() != null) {
			builder.queryParam("actor", query.actor());
		}
		if (query.year() != null) {
			builder.queryParam("year", query.year());
		}
		builder.queryParam("sortBy", query.sortBy());
		builder.queryParam("dir", query.dir());
		builder.queryParam("page", page);
		builder.queryParam("size", query.size());
		return builder.encode().toUriString();
	}

	private VoteBadges voteBadges() {
		Set<Long> liked = new HashSet<>();
		Set<Long> disliked = new HashSet<>();
		Optional<MoviePlatformUser> user = currentUser.optional();
		if (user.isEmpty()) {
			return new VoteBadges(liked, disliked);
		}
		try {
			for (VoteItem vote : voteClient.listVotes(user.get().getId())) {
				if (vote.liked()) {
					liked.add(vote.movieId());
				}
				else {
					disliked.add(vote.movieId());
				}
			}
		}
		catch (RemoteGrpcException exception) {
			logger.warn("Vote badges degraded: remote call failed with {}", exception.code(), exception);
		}
		return new VoteBadges(liked, disliked);
	}

	private record VoteBadges(Set<Long> liked, Set<Long> disliked) {
	}

}
