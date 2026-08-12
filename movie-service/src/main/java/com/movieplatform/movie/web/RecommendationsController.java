package com.movieplatform.movie.web;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.movieplatform.movie.catalog.entity.Movie;
import com.movieplatform.movie.catalog.repository.MovieRepository;
import com.movieplatform.movie.remote.grpc.RecommendationClient;
import com.movieplatform.movie.remote.grpc.RecommendationItem;
import com.movieplatform.movie.remote.grpc.RemoteGrpcException;
import com.movieplatform.movie.remote.grpc.VoteClient;
import com.movieplatform.movie.remote.grpc.VoteItem;
import com.movieplatform.movie.security.session.CurrentUser;
import com.movieplatform.movie.util.TmdbImageUrl;

@Controller
@SuppressWarnings("unused")
public class RecommendationsController {

	private static final Logger logger = LoggerFactory.getLogger(RecommendationsController.class);

	private static final int DISPLAY_LIMIT = 20;

	private final VoteClient voteClient;

	private final RecommendationClient recommendationClient;

	private final MovieRepository movieRepository;

	private final CurrentUser currentUser;

	private final TmdbImageUrl tmdbImageUrl;

	public RecommendationsController(VoteClient voteClient, RecommendationClient recommendationClient,
			MovieRepository movieRepository, CurrentUser currentUser, TmdbImageUrl tmdbImageUrl) {
		this.voteClient = voteClient;
		this.recommendationClient = recommendationClient;
		this.movieRepository = movieRepository;
		this.currentUser = currentUser;
		this.tmdbImageUrl = tmdbImageUrl;
	}

	@GetMapping("/recommendations")
	public String recommendations(Model model) {
		long userId = currentUser.require().getId();

		List<VoteItem> votes;
		try {
			votes = voteClient.listVotes(userId);
		}
		catch (RemoteGrpcException exception) {
			model.addAttribute("errorMessage", errorMessage(exception));
			return "recommendations";
		}

		List<Long> likedMovieIds = votes.stream()
				.filter(VoteItem::liked)
				.map(VoteItem::movieId)
				.toList();
		List<Long> likedTmdbIds = movieRepository.findAllById(likedMovieIds).stream()
				.map(Movie::getTmdbId)
				.filter(Objects::nonNull)
				.toList();

		if (likedTmdbIds.isEmpty()) {
			model.addAttribute("noLikedMovies", true);
			return "recommendations";
		}

		List<RecommendationItem> items;
		try {
			items = recommendationClient.recommendations(likedTmdbIds);
		}
		catch (RemoteGrpcException exception) {
			model.addAttribute("errorMessage", errorMessage(exception));
			return "recommendations";
		}

		Set<Long> votedMovieIds = votes.stream()
				.map(VoteItem::movieId)
				.collect(Collectors.toSet());
		Map<Long, Long> movieIdByTmdbId = movieRepository.findByTmdbIdIn(
						items.stream().map(RecommendationItem::tmdbId).toList()).stream()
				.collect(Collectors.toMap(Movie::getTmdbId, Movie::getId, (first, second) -> first));

		List<RecommendationView> views = items.stream()
				.map(item -> {
					Long catalogMovieId = movieIdByTmdbId.get(item.tmdbId());
					if (catalogMovieId == null || votedMovieIds.contains(catalogMovieId)) {
						return null;
					}
					return new RecommendationView(
							catalogMovieId, item.tmdbId(), item.title(), item.year(), item.posterPath(),
							tmdbImageUrl.poster(item.posterPath()),
							item.overview(), (int) Math.round(item.score() * 100));
				})
				.filter(Objects::nonNull)
				.limit(DISPLAY_LIMIT)
				.toList();

		if (views.isEmpty()) {
			model.addAttribute("noRecommendations", true);
			return "recommendations";
		}

		model.addAttribute("recommendations", views);
		return "recommendations";
	}

	private String errorMessage(RemoteGrpcException exception) {
		logger.warn("Recommendations page degraded: remote call failed with {}", exception.code(), exception);
		return switch (exception.code()) {
			case RESOURCE_EXHAUSTED -> "TMDb rate limit exceeded — please try again in a moment.";
			case DEADLINE_EXCEEDED -> "The recommendation lookup timed out — please try again.";
			case UNAVAILABLE -> "The recommendation service is currently unavailable.";
			default -> "Something went wrong while loading recommendations.";
		};
	}

	public record RecommendationView(long movieId, long tmdbId, String title, int year, String posterPath,
			String posterUrl, String overview, int score) {
	}

}
