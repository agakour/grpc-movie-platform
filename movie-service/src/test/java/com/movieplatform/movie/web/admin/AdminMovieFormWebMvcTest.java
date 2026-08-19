package com.movieplatform.movie.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.movieplatform.movie.catalog.service.MovieService;
import com.movieplatform.movie.remote.tmdb.TmdbMovieDetailsClient;
import com.movieplatform.movie.remote.tmdb.TmdbMovieEnrichment;
import com.movieplatform.movie.remote.tmdb.TmdbSearchClient;
import com.movieplatform.movie.remote.tmdb.TmdbSearchResult;
import com.movieplatform.movie.security.AppUserDetailsService;
import com.movieplatform.movie.security.SecurityConfig;
import com.movieplatform.movie.testutil.MovieMvcSecurityTestCustomizer;
import com.movieplatform.movie.util.TmdbImageUrl;

@WebMvcTest(controllers = AdminMovieController.class)
@Import({TmdbImageUrl.class,
		SecurityConfig.class,
		AdminMovieFormWebMvcTest.SecurityMockMvcConfig.class,
		SecurityAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class})
@SuppressWarnings("unused")
class AdminMovieFormWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MovieService movieService;

	@MockitoBean
	private TmdbSearchClient tmdbSearchClient;

	@MockitoBean
	private TmdbMovieDetailsClient tmdbMovieDetailsClient;

	@MockitoBean
	private AppUserDetailsService appUserDetailsService;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@TestConfiguration(proxyBeanMethods = false)
	@SuppressWarnings("unused")
	static class SecurityMockMvcConfig {

		@Bean
		MovieMvcSecurityTestCustomizer securityMockMvcCustomizer() {
			return new MovieMvcSecurityTestCustomizer();
		}

	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void lookupRendersCandidateListWhenTmdbOmitsTheReleaseDate() throws Exception {
		when(tmdbSearchClient.search("Ghost", null)).thenReturn(List.of(
				new TmdbSearchResult(11L, "Ghost", "Ghost", "",
						"/p1.jpg", "/b1.jpg", "Undated entry", "en"),
				new TmdbSearchResult(22L, "Ghost Story", "Ghost Story", "1981-03-13",
						null, null, "Dated entry", "en")));
		when(tmdbMovieDetailsClient.enrich(11L)).thenReturn(new TmdbMovieEnrichment(
				11L, "Ghost", "Ghost", "Undated entry", null, null,
				"en", "/p1.jpg", "/b1.jpg",
				List.of("Jerry Zucker"),
				List.of("Patrick Swayze", "Demi Moore"),
				List.of("Romance", "Fantasy"),
				List.of("Haunting")));

		mockMvc.perform(post("/admin/movies").with(csrf())
						.param("title", "")
						.param("originalTitle", "Ghost")
						.param("releaseYear", "")
						.param("runtimeMinutes", "")
						.param("tmdbId", "")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ghost Story")))
				.andExpect(content().string(containsString("(1981)")))
				.andExpect(content().string(containsString("value=\"11\"")))
				.andExpect(content().string(containsString("/p1.jpg")))
				.andExpect(content().string(containsString("class=\"no-art\"")))
				.andExpect(content().string(containsString("Patrick Swayze")))
				.andExpect(content().string(containsString("Jerry Zucker")));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void lookupWithoutMatchesExplainsThatNothingWasFound() throws Exception {
		when(tmdbSearchClient.search("Nothing Like This", null)).thenReturn(List.of());

		mockMvc.perform(post("/admin/movies").with(csrf())
						.param("title", "")
						.param("originalTitle", "Nothing Like This")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("No TMDb match for that original title")))
				.andExpect(content().string(not(containsString("pick the matching result"))));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void lookupRendersOnlyTheFiveBestCandidates() throws Exception {
		when(tmdbSearchClient.search("Ghost", null)).thenReturn(List.of(
				new TmdbSearchResult(11L, "Ghost", "Ghost", "",
						null, null, "Undated entry", "en"),
				new TmdbSearchResult(22L, "Ghost Story", "Ghost Story", "1981-03-13",
						null, null, "Dated entry", "en"),
				new TmdbSearchResult(33L, "Ghost 3", "Ghost 3", "1990-01-01", null, null, "Third", "en"),
				new TmdbSearchResult(44L, "Ghost 4", "Ghost 4", "1990-01-01", null, null, "Fourth", "en"),
				new TmdbSearchResult(55L, "Ghost 5", "Ghost 5", "1990-01-01", null, null, "Fifth", "en"),
				new TmdbSearchResult(66L, "Ghost 6", "Ghost 6", "1990-01-01", null, null, "Sixth", "en"),
				new TmdbSearchResult(77L, "Ghost 7", "Ghost 7", "1990-01-01", null, null, "Seventh", "en")));
		when(tmdbMovieDetailsClient.enrich(11L)).thenReturn(new TmdbMovieEnrichment(
				11L, "Ghost", "Ghost", "Undated entry", null, null,
				"en", null, null, List.of(), List.of(), List.of(), List.of()));

		mockMvc.perform(post("/admin/movies").with(csrf())
						.param("title", "")
						.param("originalTitle", "Ghost")
						.param("action", "lookup"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ghost 5")))
				.andExpect(content().string(not(containsString("Ghost 6"))))
				.andExpect(content().string(not(containsString("Ghost 7"))));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void pickBackfillsTheChosenCandidateAndHidesTheCandidateList() throws Exception {
		when(tmdbMovieDetailsClient.enrich(22L)).thenReturn(new TmdbMovieEnrichment(
				22L, "Ghost Story", "Ghost Story", "Dated entry", 1981, 110,
				"en", "/p2.jpg", "/b2.jpg",
				List.of("John Irvin"),
				List.of("Craig Wasson"),
				List.of("Horror"),
				List.of("Haunting")));

		mockMvc.perform(post("/admin/movies").with(csrf())
						.param("title", "")
						.param("originalTitle", "Ghost")
						.param("tmdbId", "22")
						.param("action", "pick"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"22\"")))
				.andExpect(content().string(containsString("Ghost Story")))
				.andExpect(content().string(containsString("John Irvin")))
				.andExpect(content().string(containsString("Haunting")))
				.andExpect(content().string(not(containsString("pick the matching result"))));
		verify(tmdbSearchClient, never()).search(any(), any());
	}

}
