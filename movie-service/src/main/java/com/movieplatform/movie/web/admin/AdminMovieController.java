package com.movieplatform.movie.web.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.movieplatform.movie.catalog.dto.MovieListQuery;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.dto.MovieSummaryResponse;
import com.movieplatform.movie.catalog.dto.PageResponse;
import com.movieplatform.movie.catalog.service.MovieService;
import com.movieplatform.movie.exception.DuplicateMovieException;
import com.movieplatform.movie.exception.NotFoundException;
import com.movieplatform.movie.remote.tmdb.TmdbDetailsException;
import com.movieplatform.movie.remote.tmdb.TmdbMovieDetailsClient;
import com.movieplatform.movie.remote.tmdb.TmdbMovieEnrichment;
import com.movieplatform.movie.remote.tmdb.TmdbSearchClient;
import com.movieplatform.movie.remote.tmdb.TmdbSearchException;
import com.movieplatform.movie.remote.tmdb.TmdbSearchResult;
import com.movieplatform.movie.util.TmdbImageUrl;

import jakarta.validation.Validator;

@Controller
@RequestMapping("/admin/movies")
@SuppressWarnings("unused")
public class AdminMovieController {

	private static final String LOOKUP_ACTION = "lookup";

	private static final String PICK_ACTION = "pick";

	private static final int MAX_CANDIDATES = 5;

	private final MovieService movieService;

	private final TmdbSearchClient tmdbSearchClient;

	private final TmdbMovieDetailsClient tmdbMovieDetailsClient;

	private final Validator validator;

	private final TmdbImageUrl tmdbImageUrl;

	public AdminMovieController(MovieService movieService, TmdbSearchClient tmdbSearchClient,
			TmdbMovieDetailsClient tmdbMovieDetailsClient, Validator validator, TmdbImageUrl tmdbImageUrl) {
		this.movieService = movieService;
		this.tmdbSearchClient = tmdbSearchClient;
		this.tmdbMovieDetailsClient = tmdbMovieDetailsClient;
		this.validator = validator;
		this.tmdbImageUrl = tmdbImageUrl;
	}

	@GetMapping
	public String list(
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "size", required = false) Integer size,
			Model model) {
		int safePage = page == null || page < 0 ? 0 : page;
		int safeSize = size == null ? 20 : Math.clamp(size, 1, 100);
		String safeQ = blankToNull(q);
		MovieListQuery query = new MovieListQuery(
				null, null, null, null, null, safeQ, MovieListQuery.SORT_RELEASE_YEAR, MovieListQuery.DIR_DESC,
				safePage, safeSize);
		PageResponse<MovieSummaryResponse> result = movieService.list(query);
		if (result.totalPages() > 0 && safePage >= result.totalPages()) {
			return "redirect:" + pageUrl(safeQ, result.totalPages() - 1, safeSize);
		}
		model.addAttribute("movies", result);
		model.addAttribute("q", safeQ);
		model.addAttribute("prevPageUrl", result.page() > 0 ? pageUrl(safeQ, safePage - 1, safeSize) : null);
		model.addAttribute("nextPageUrl",
				result.page() + 1 < result.totalPages() ? pageUrl(safeQ, safePage + 1, safeSize) : null);
		return "admin/movies";
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String pageUrl(String q, int page, int size) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/movies");
		if (q != null) {
			builder.queryParam("q", q);
		}
		builder.queryParam("page", page);
		builder.queryParam("size", size);
		return builder.encode().toUriString();
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("form", AdminMovieForm.empty());
		return "admin/movie-form";
	}

	@PostMapping
	public String create(@ModelAttribute("form") AdminMovieForm form, BindingResult binding,
			RedirectAttributes redirectAttributes, Model model) {
		if (isPick(form)) {
			return pick(form, binding, model, null);
		}
		if (isLookup(form)) {
			return lookup(form, binding, model, null);
		}
		validate(form, binding);
		if (binding.hasErrors()) {
			return "admin/movie-form";
		}
		try {
			movieService.create(form.toRequest());
		}
		catch (DuplicateMovieException exception) {
			redirectAttributes.addFlashAttribute("saveFailed", "A movie with this tmdb_id already exists.");
			return "redirect:/admin/movies";
		}
		catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("saveFailed", exception.getMessage());
			return "redirect:/admin/movies";
		}
		redirectAttributes.addFlashAttribute("saved", true);
		return "redirect:/admin/movies";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("form", AdminMovieForm.fromResponse(loadMovie(id)));
		model.addAttribute("movieId", id);
		return "admin/movie-form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @ModelAttribute("form") AdminMovieForm form,
			BindingResult binding, RedirectAttributes redirectAttributes, Model model) {
		model.addAttribute("movieId", id);
		if (isPick(form)) {
			return pick(form, binding, model, id);
		}
		if (isLookup(form)) {
			return lookup(form, binding, model, id);
		}
		validate(form, binding);
		if (binding.hasErrors()) {
			return "admin/movie-form";
		}
		try {
			movieService.update(id, form.toRequest());
		}
		catch (NotFoundException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown movie " + id, exception);
		}
		catch (DuplicateMovieException exception) {
			redirectAttributes.addFlashAttribute("saveFailed", "A movie with this tmdb_id already exists.");
			return "redirect:/admin/movies";
		}
		catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("saveFailed", exception.getMessage());
			return "redirect:/admin/movies";
		}
		redirectAttributes.addFlashAttribute("saved", true);
		return "redirect:/admin/movies";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			movieService.delete(id);
		}
		catch (NotFoundException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown movie " + id, exception);
		}
		redirectAttributes.addFlashAttribute("deleted", true);
		return "redirect:/admin/movies";
	}

	private boolean isLookup(AdminMovieForm form) {
		return LOOKUP_ACTION.equals(form.action());
	}

	private boolean isPick(AdminMovieForm form) {
		return PICK_ACTION.equals(form.action());
	}

	private String pick(AdminMovieForm form, BindingResult binding, Model model, Long movieId) {
		if (movieId != null) {
			model.addAttribute("movieId", movieId);
		}
		if (form.tmdbId() == null) {
			binding.rejectValue("tmdbId", "adminMovie.pick.missing",
					"Pick a TMDb candidate to fill in the form.");
			return "admin/movie-form";
		}
		model.addAttribute("picked", true);
		try {
			TmdbMovieEnrichment enrichment = tmdbMovieDetailsClient.enrich(form.tmdbId());
			model.addAttribute("form", form.withFullBackfill(enrichment));
		}
		catch (TmdbDetailsException exception) {
			binding.rejectValue("tmdbId", "adminMovie.pick.failed",
					"Could not load details for the picked TMDb ID — try again or enter them manually.");
		}
		return "admin/movie-form";
	}

	private String lookup(AdminMovieForm form, BindingResult binding, Model model, Long movieId) {
		if (movieId != null) {
			model.addAttribute("movieId", movieId);
		}
		if (form.originalTitle() == null || form.originalTitle().isBlank()) {
			binding.rejectValue("originalTitle", "adminMovie.lookup.originalTitle",
					"Enter an original title to search TMDb.");
			return "admin/movie-form";
		}
		List<TmdbSearchResult> candidates;
		try {
			candidates = tmdbSearchClient.search(form.originalTitle(), form.releaseYear())
					.stream()
					.limit(MAX_CANDIDATES)
					.toList();
		}
		catch (TmdbSearchException exception) {
			binding.rejectValue("originalTitle", "adminMovie.lookup.failed",
					"TMDb lookup failed — enter the details manually.");
			return "admin/movie-form";
		}
		model.addAttribute("candidates", candidates);
		model.addAttribute("posterUrls", posterUrls(candidates));
		model.addAttribute("candidateYears", candidateYears(candidates));
		TmdbSearchResult target = pickTarget(form, candidates);
		if (target != null) {
			try {
				TmdbMovieEnrichment enrichment = tmdbMovieDetailsClient.enrich(target.id());
				model.addAttribute("form", form.withFullBackfill(enrichment));
			}
			catch (TmdbDetailsException exception) {
				binding.rejectValue("originalTitle", "adminMovie.lookup.failed",
						"TMDb lookup failed — enter the details manually.");
			}
		}
		return "admin/movie-form";
	}

	private TmdbSearchResult pickTarget(AdminMovieForm form, List<TmdbSearchResult> candidates) {
		if (form.tmdbId() != null) {
			for (TmdbSearchResult candidate : candidates) {
				if (form.tmdbId().equals(candidate.id())) {
					return candidate;
				}
			}
		}
		return bestMatch(form, candidates);
	}

	private TmdbSearchResult bestMatch(AdminMovieForm form, List<TmdbSearchResult> candidates) {
		String originalTitle = form.originalTitle();
		Integer year = form.releaseYear();
		for (TmdbSearchResult candidate : candidates) {
			boolean titleMatch = originalTitle != null
					&& (originalTitle.equals(candidate.title()) || originalTitle.equals(candidate.originalTitle()));
			boolean yearMatch = year == null || year.equals(releaseYear(candidate.releaseDate()));
			if (titleMatch && yearMatch) {
				return candidate;
			}
		}
		return null;
	}

	private Map<Long, String> posterUrls(List<TmdbSearchResult> candidates) {
		Map<Long, String> urls = new HashMap<>();
		for (TmdbSearchResult candidate : candidates) {
			urls.put(candidate.id(), tmdbImageUrl.poster(candidate.posterPath()));
		}
		return urls;
	}

	private Map<Long, Integer> candidateYears(List<TmdbSearchResult> candidates) {
		Map<Long, Integer> years = new HashMap<>();
		for (TmdbSearchResult candidate : candidates) {
			Integer year = releaseYear(candidate.releaseDate());
			if (year != null) {
				years.put(candidate.id(), year);
			}
		}
		return years;
	}

	private void validate(AdminMovieForm form, BindingResult binding) {
		new SpringValidatorAdapter(validator).validate(form, binding);
	}

	private static Integer releaseYear(String releaseDate) {

		if (releaseDate == null || releaseDate.length() < 4) {
			return null;
		}
		try {
			return Integer.parseInt(releaseDate.substring(0, 4));
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private MovieResponse loadMovie(Long id) {
		try {
			return movieService.get(id);
		}
		catch (NotFoundException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown movie " + id, exception);
		}
	}

}
