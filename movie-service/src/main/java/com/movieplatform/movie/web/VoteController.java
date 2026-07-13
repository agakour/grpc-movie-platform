package com.movieplatform.movie.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.movieplatform.movie.catalog.repository.MovieRepository;
import com.movieplatform.movie.remote.grpc.RemoteGrpcException;
import com.movieplatform.movie.remote.grpc.VoteClient;
import com.movieplatform.movie.security.session.CurrentUser;

@Controller
@SuppressWarnings("unused")
public class VoteController {

	private final VoteClient voteClient;

	private final MovieRepository movieRepository;

	private final CurrentUser currentUser;

	public VoteController(VoteClient voteClient, MovieRepository movieRepository, CurrentUser currentUser) {
		this.voteClient = voteClient;
		this.movieRepository = movieRepository;
		this.currentUser = currentUser;
	}

	@PostMapping("/movies/{id}/vote")
	public String vote(@PathVariable Long id,
			@RequestParam(name = "liked", defaultValue = "true") boolean liked,
			RedirectAttributes redirectAttributes) {
		if (!movieRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown movie " + id);
		}
		try {
			voteClient.setVote(currentUser.require().getId(), id, liked);
		}
		catch (RemoteGrpcException exception) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"The vote store is currently unavailable.", exception);
		}
		redirectAttributes.addFlashAttribute("voted", liked);
		return "redirect:/movies/" + id;
	}

}
