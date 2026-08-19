package com.movieplatform.movie.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;

import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.service.MovieService;
import com.movieplatform.movie.exception.DuplicateMovieException;
import com.movieplatform.movie.remote.tmdb.TmdbDetailsException;
import com.movieplatform.movie.remote.tmdb.TmdbMovieDetailsClient;
import com.movieplatform.movie.remote.tmdb.TmdbMovieEnrichment;
import com.movieplatform.movie.remote.tmdb.TmdbSearchClient;
import com.movieplatform.movie.remote.tmdb.TmdbSearchException;
import com.movieplatform.movie.remote.tmdb.TmdbSearchResult;
import com.movieplatform.movie.util.TmdbImageUrl;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class AdminMovieControllerTest {

	@Mock
	private MovieService movieService;

	@Mock
	private TmdbSearchClient tmdbSearchClient;

	@Mock
	private TmdbMovieDetailsClient tmdbMovieDetailsClient;

	private MockMvc mockMvc;

	private ValidatorFactory validatorFactory;

	@BeforeEach
	void setUp() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		AdminMovieController controller = new AdminMovieController(movieService, tmdbSearchClient,
				tmdbMovieDetailsClient, validatorFactory.getValidator(), new TmdbImageUrl());
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@AfterEach
	void tearDown() {
		validatorFactory.close();
	}

	private static Map<String, Object> modelOf(MvcResult result) {
		return Objects.requireNonNull(result.getModelAndView(), "expected a ModelAndView").getModel();
	}

	@Test
	void lookupBackfillsCandidateFieldsAndKeepsSearchInputs() throws Exception {
		when(tmdbSearchClient.search("Inception", 2010)).thenReturn(List.of(
				new TmdbSearchResult(27205L, "Inception", "Inception", "2010-07-15",
						"/p.jpg", "/b.jpg", "A thief who steals corporate secrets.", "en")));
		when(tmdbMovieDetailsClient.enrich(27205L)).thenReturn(new TmdbMovieEnrichment(
				27205L, "Inception", "Inception", "A thief who steals corporate secrets.", 2010, 148,
				"en", "/p.jpg", "/b.jpg",
				List.of("Christopher Nolan"),
				List.of("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page", "Tom Hardy",
						"Ken Watanabe", "Dileep Rao", "Cillian Murphy", "Tom Berenger"),
				List.of("Action", "Science Fiction", "Thriller"),
				List.of("Dream", "Heist")));

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Inception")
						.param("releaseYear", "2010")
						.param("title", "")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		Map<String, Object> model = modelOf(result);
		AdminMovieForm form = (AdminMovieForm) model.get("form");
		assertThat(form.tmdbId()).isEqualTo(27205L);
		assertThat(form.title()).isEqualTo("Inception");
		assertThat(form.overview()).isEqualTo("A thief who steals corporate secrets.");
		assertThat(form.posterPath()).isEqualTo("/p.jpg");
		assertThat(form.backdropPath()).isEqualTo("/b.jpg");
		assertThat(form.originalLanguage()).isEqualTo("en");
		assertThat(form.originalTitle()).isEqualTo("Inception");
		assertThat(form.releaseYear()).isEqualTo(2010);
		assertThat(form.runtimeMinutes()).isEqualTo(148);
		assertThat(form.directors()).isEqualTo("Christopher Nolan");
		assertThat(form.cast()).isEqualTo("""
				Leonardo DiCaprio
				Joseph Gordon-Levitt
				Elliot Page
				Tom Hardy
				Ken Watanabe
				Dileep Rao
				Cillian Murphy
				Tom Berenger""");
		assertThat(form.genres()).isEqualTo("""
				Action
				Science Fiction
				Thriller""");
		assertThat(form.keywords()).isEqualTo("""
				Dream
				Heist""");

		@SuppressWarnings("unchecked")
		Map<Long, String> posterUrls = (Map<Long, String>) model.get("posterUrls");
		assertThat(posterUrls).containsEntry(27205L, "https://image.tmdb.org/t/p/w342/p.jpg");
	}

	@Test
	void candidateYearsSkipCandidatesTmdbReturnsWithoutAReleaseDate() throws Exception {
		when(tmdbSearchClient.search("Ghost", null)).thenReturn(List.of(
				new TmdbSearchResult(11L, "Ghost", "Ghost", "",
						"/p1.jpg", "/b1.jpg", "Undated entry", "en"),
				new TmdbSearchResult(22L, "Ghost Story", "Ghost Story", "1981-03-13",
						null, null, "Dated entry", "en")));
		when(tmdbMovieDetailsClient.enrich(11L)).thenReturn(new TmdbMovieEnrichment(
				11L, "Ghost", "Ghost", "Undated entry", null, null,
				"en", "/p1.jpg", "/b1.jpg", List.of(), List.of(), List.of(), List.of()));

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Ghost")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andReturn();

		@SuppressWarnings("unchecked")
		Map<Long, Integer> candidateYears = (Map<Long, Integer>) modelOf(result).get("candidateYears");
		assertThat(candidateYears).containsExactly(Map.entry(22L, 1981));
	}

	@Test
	void rePickedRadioCandidateBackfillsFromThatCandidate() throws Exception {
		when(tmdbSearchClient.search("Inception", 2010)).thenReturn(List.of(
				new TmdbSearchResult(27205L, "Inception", "Inception", "2010-07-15",
						"/p1.jpg", "/b1.jpg", "Overview one", "en"),
				new TmdbSearchResult(99999L, "Inception (Other)", "Inception", "2010-06-01",
						"/p2.jpg", "/b2.jpg", "Overview two", "fr")));
		when(tmdbMovieDetailsClient.enrich(99999L)).thenReturn(new TmdbMovieEnrichment(
				99999L, "Inception (Other)", "Inception", "Overview two", 2010, 100,
				"fr", "/p2.jpg", "/b2.jpg", List.of(), List.of(), List.of(), List.of()));

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Inception")
						.param("releaseYear", "2010")
						.param("tmdbId", "99999")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		AdminMovieForm form = (AdminMovieForm) modelOf(result).get("form");
		assertThat(form.tmdbId()).isEqualTo(99999L);
		assertThat(form.title()).isEqualTo("Inception (Other)");
		assertThat(form.overview()).isEqualTo("Overview two");
		assertThat(form.posterPath()).isEqualTo("/p2.jpg");
		assertThat(form.backdropPath()).isEqualTo("/b2.jpg");
		assertThat(form.originalLanguage()).isEqualTo("fr");
	}

	@Test
	void pickBackfillsFromThePickedCandidateWithoutRerunningTheSearch() throws Exception {
		when(tmdbMovieDetailsClient.enrich(99999L)).thenReturn(new TmdbMovieEnrichment(
				99999L, "Inception (Other)", "Inception", "Overview two", 2009, 100,
				"fr", "/p2.jpg", "/b2.jpg", List.of(), List.of(), List.of(), List.of()));

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Inception")
						.param("releaseYear", "2010")
						.param("tmdbId", "99999")
						.param("action", "pick"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		AdminMovieForm form = (AdminMovieForm) modelOf(result).get("form");
		assertThat(form.tmdbId()).isEqualTo(99999L);
		assertThat(form.title()).isEqualTo("Inception (Other)");
		assertThat(form.originalLanguage()).isEqualTo("fr");
		assertThat(form.releaseYear()).isEqualTo(2009);
		verify(tmdbSearchClient, never()).search(any(), any());
	}

	@Test
	void pickWithoutATmdbIdRejectsWithAFieldError() throws Exception {
		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Inception")
						.param("action", "pick"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		BindingResult binding = (BindingResult) modelOf(result).get(BindingResult.MODEL_KEY_PREFIX + "form");
		assertThat(binding.hasFieldErrors("tmdbId")).isTrue();
		verify(tmdbMovieDetailsClient, never()).enrich(anyLong());
	}

	@Test
	void pickFailureRendersFriendlyErrorWithoutBackfill() throws Exception {
		when(tmdbMovieDetailsClient.enrich(27205L))
				.thenThrow(new TmdbDetailsException(27205L, new RuntimeException("boom")));

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Inception")
						.param("tmdbId", "27205")
						.param("action", "pick"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		AdminMovieForm form = (AdminMovieForm) modelOf(result).get("form");
		assertThat(form.runtimeMinutes()).isNull();
		BindingResult binding = (BindingResult) modelOf(result).get(BindingResult.MODEL_KEY_PREFIX + "form");
		assertThat(binding.hasFieldErrors("tmdbId")).isTrue();
	}

	@Test
	void lookupShowsOnlyTheFiveBestCandidates() throws Exception {
		List<TmdbSearchResult> many = new ArrayList<>();
		for (long id = 1; id <= 7; id++) {
			many.add(new TmdbSearchResult(id, "Result " + id, "Result " + id, "2010-01-01",
					null, null, "Overview " + id, "en"));
		}
		when(tmdbSearchClient.search("Result", null)).thenReturn(many);

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Result")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<TmdbSearchResult> candidates = (List<TmdbSearchResult>) modelOf(result).get("candidates");
		assertThat(candidates).hasSize(5);
		assertThat(candidates.get(0).id()).isEqualTo(1L);
		assertThat(candidates.get(4).id()).isEqualTo(5L);
	}

	@Test
	void lookupFailureRendersFriendlyFieldErrorWithoutBackfill() throws Exception {
		when(tmdbSearchClient.search("Unknown", null))
				.thenThrow(new TmdbSearchException("Unknown", new RuntimeException("boom")));

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Unknown")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		Map<String, Object> model = modelOf(result);
		assertThat(model.get("candidates")).isNull();
		AdminMovieForm form = (AdminMovieForm) model.get("form");
		assertThat(form.tmdbId()).isNull();
		BindingResult binding = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "form");
		assertThat(binding.hasFieldErrors("originalTitle")).isTrue();
	}

	@Test
	void enrichmentFailureRendersFriendlyFieldErrorWithoutBackfill() throws Exception {
		when(tmdbSearchClient.search("Inception", 2010)).thenReturn(List.of(
				new TmdbSearchResult(27205L, "Inception", "Inception", "2010-07-15",
						"/p.jpg", "/b.jpg", "A thief who steals corporate secrets.", "en")));
		when(tmdbMovieDetailsClient.enrich(27205L))
				.thenThrow(new TmdbDetailsException(27205L, new RuntimeException("boom")));

		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("originalTitle", "Inception")
						.param("releaseYear", "2010")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		Map<String, Object> model = modelOf(result);
		AdminMovieForm form = (AdminMovieForm) model.get("form");
		assertThat(form.tmdbId()).isNull();
		assertThat(form.runtimeMinutes()).isNull();
		assertThat(form.directors()).isNullOrEmpty();
		BindingResult binding = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "form");
		assertThat(binding.hasFieldErrors("originalTitle")).isTrue();
	}

	@Test
	void saveStillRequiresTmdbId() throws Exception {
		MvcResult result = mockMvc.perform(post("/admin/movies")
						.param("title", "New Movie")
						.param("originalTitle", "New Movie")
						.param("releaseYear", "2010")
						.param("runtimeMinutes", "100")
						.param("action", "save"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movie-form"))
				.andReturn();

		BindingResult binding = (BindingResult) modelOf(result).get(BindingResult.MODEL_KEY_PREFIX + "form");
		assertThat(binding.hasFieldErrors("tmdbId")).isTrue();
		verify(movieService, never()).create(any(MovieRequest.class));
	}

	@Test
	void saveWithTmdbIdCreatesMovie() throws Exception {
		when(movieService.create(any(MovieRequest.class))).thenReturn(new MovieResponse(
				42L, "New Movie", "New Movie", null, 2010, 100, "en", null, null, 1L,
				List.of(), List.of(), List.of(), List.of()));

		mockMvc.perform(post("/admin/movies")
						.param("title", "New Movie")
						.param("originalTitle", "New Movie")
						.param("releaseYear", "2010")
						.param("runtimeMinutes", "100")
						.param("originalLanguage", "en")
						.param("tmdbId", "1")
						.param("action", "save"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/movies"));

		verify(movieService).create(any(MovieRequest.class));
	}

	@Test
	void updateSaveRedirectsToMovies() throws Exception {
		mockMvc.perform(post("/admin/movies/42")
						.param("title", "New Movie")
						.param("originalTitle", "New Movie")
						.param("releaseYear", "2010")
						.param("runtimeMinutes", "100")
						.param("originalLanguage", "en")
						.param("tmdbId", "1")
						.param("action", "save"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/movies"));

		verify(movieService).update(eq(42L), any(MovieRequest.class));
	}

	@Test
	void saveDuplicateTmdbIdRedirectsToMovies() throws Exception {
		when(movieService.create(any(MovieRequest.class)))
				.thenThrow(new DuplicateMovieException("uq_movie_tmdb_id"));

		mockMvc.perform(post("/admin/movies")
						.param("title", "New Movie")
						.param("originalTitle", "New Movie")
						.param("releaseYear", "2010")
						.param("runtimeMinutes", "100")
						.param("originalLanguage", "en")
						.param("tmdbId", "1")
						.param("action", "save"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/movies"));
	}

}
