package com.movieplatform.movie.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.service.MovieService;
import com.movieplatform.movie.exception.NotFoundException;
import com.movieplatform.movie.remote.grpc.RemoteGrpcException;
import com.movieplatform.movie.remote.grpc.VoteClient;
import com.movieplatform.movie.security.session.CurrentUser;
import com.movieplatform.movie.util.TmdbImageUrl;

@Controller
@SuppressWarnings("unused")
public class MovieWebController {

	private static final Logger logger = LoggerFactory.getLogger(MovieWebController.class);

	private final MovieService movieService;

	private final VoteClient voteClient;

	private final CurrentUser currentUser;

	private final TmdbImageUrl tmdbImageUrl;

	public MovieWebController(MovieService movieService, VoteClient voteClient, CurrentUser currentUser,
			TmdbImageUrl tmdbImageUrl) {
		this.movieService = movieService;
		this.voteClient = voteClient;
		this.currentUser = currentUser;
		this.tmdbImageUrl = tmdbImageUrl;
	}

	@GetMapping("/movies/{id}")
	public String detail(@PathVariable Long id, Model model) {
		MovieResponse movie;
		try {
			movie = movieService.get(id);
		}
		catch (NotFoundException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown movie " + id, exception);
		}
		model.addAttribute("movie", movie);
		model.addAttribute("posterUrl", tmdbImageUrl.posterLarge(movie.posterPath()));
		model.addAttribute("backdropUrl", tmdbImageUrl.backdrop(movie.backdropPath()));

		Boolean liked = null;
		if (currentUser.optional().isPresent()) {
			try {
				liked = voteClient.getVote(currentUser.require().getId(), id).orElse(null);
			}
			catch (RemoteGrpcException exception) {
				logger.warn("Movie detail vote state degraded: remote call failed with {}", exception.code(), exception);
			}
		}
		model.addAttribute("voteState", liked);
		return "movie-detail";
	}

}
