package com.movieplatform.movie.catalog.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.movieplatform.movie.catalog.dto.MovieListQuery;
import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.dto.MovieSummaryResponse;
import com.movieplatform.movie.catalog.dto.PageResponse;
import com.movieplatform.movie.catalog.service.MovieService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/movies")
@SuppressWarnings("unused")
public class MovieController {

	private final MovieService movieService;

	public MovieController(MovieService movieService) {
		this.movieService = movieService;
	}

	@GetMapping
	public PageResponse<MovieSummaryResponse> list(@ModelAttribute @Valid MovieListQuery query) {
		return movieService.list(query);
	}

	@GetMapping("/{id}")
	public MovieResponse get(@PathVariable Long id) {
		return movieService.get(id);
	}

	@PostMapping
	public ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
		MovieResponse created = movieService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public MovieResponse update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
		return movieService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		movieService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
