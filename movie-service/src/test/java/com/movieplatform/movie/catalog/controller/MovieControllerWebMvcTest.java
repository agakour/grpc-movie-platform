package com.movieplatform.movie.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import com.movieplatform.movie.testutil.MovieMvcSecurityTestCustomizer;

import com.movieplatform.movie.security.AppUserDetailsService;
import com.movieplatform.movie.security.SecurityConfig;
import com.movieplatform.movie.exception.DuplicateMovieException;
import com.movieplatform.movie.exception.NotFoundException;
import com.movieplatform.movie.catalog.dto.MovieListQuery;
import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.catalog.dto.MovieResponse;
import com.movieplatform.movie.catalog.dto.MovieSummaryResponse;
import com.movieplatform.movie.catalog.dto.PageResponse;
import com.movieplatform.movie.catalog.service.MovieService;

@WebMvcTest(controllers = MovieController.class)
@Import({SecurityConfig.class,
		MovieControllerWebMvcTest.SecurityMockMvcConfig.class,
		SecurityAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class})
@WithMockUser
@SuppressWarnings("unused")
class MovieControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MovieService movieService;

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
	void listReturnsPagedSummaries() throws Exception {
		PageResponse<MovieSummaryResponse> page = new PageResponse<>(
				List.of(new MovieSummaryResponse(1L, "Seven Samurai", "七人の侍", 1954, 207, "ja", "/p.jpg")),
				0, 3, 1340, 447);
		when(movieService.list(any(MovieListQuery.class))).thenReturn(page);

		mockMvc.perform(get("/api/movies")
						.param("page", "0")
						.param("size", "3")
						.param("sortBy", "releaseYear")
						.param("dir", "desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].title").value("Seven Samurai"))
				.andExpect(jsonPath("$.totalElements").value(1340))
				.andExpect(jsonPath("$.totalPages").value(447));
	}

	@Test
	void listAppliesDefaultPagingWhenParamsAreAbsent() throws Exception {
		when(movieService.list(any(MovieListQuery.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 1340, 67));

		mockMvc.perform(get("/api/movies"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.page").value(0));
	}

	@Test
	void listRejectsNegativePage() throws Exception {
		mockMvc.perform(get("/api/movies").param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("urn:problem:validation-error"))
				.andExpect(jsonPath("$.errors.page").exists());
	}

	@Test
	void listRejectsOversizedPage() throws Exception {
		mockMvc.perform(get("/api/movies").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.size").exists());
	}

	@Test
	void listRejectsZeroPageSize() throws Exception {
		mockMvc.perform(get("/api/movies").param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.size").exists());
	}

	@Test
	void listRejectsUnknownSortBy() throws Exception {
		when(movieService.list(any(MovieListQuery.class)))
				.thenThrow(new IllegalArgumentException("Unknown sortBy: 'bogus'"));

		mockMvc.perform(get("/api/movies").param("sortBy", "bogus"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
	}

	@Test
	void getReturnsMovie() throws Exception {
		MovieResponse response = new MovieResponse(5L, "Seven Samurai", "七人の侍", null, 1954, 207, "ja",
				"/p.jpg", null, 346L, List.of("Akira Kurosawa"), List.of(), List.of("action", "drama"), List.of());
		when(movieService.get(5L)).thenReturn(response);

		mockMvc.perform(get("/api/movies/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.tmdbId").value(346))
				.andExpect(jsonPath("$.directors[0]").value("Akira Kurosawa"))
				.andExpect(jsonPath("$.genres[1]").value("drama"));
	}

	@Test
	void getMissingMovieMapsTo404() throws Exception {
		when(movieService.get(404L)).thenThrow(new NotFoundException("movie", 404L));

		mockMvc.perform(get("/api/movies/404"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("urn:problem:not-found"))
				.andExpect(jsonPath("$.resource").value("movie"))
				.andExpect(jsonPath("$.id").value(404));
	}

	@Test
	void getNonNumericIdMapsTo400() throws Exception {
		mockMvc.perform(get("/api/movies/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createReturns201WithLocation() throws Exception {
		MovieResponse created = new MovieResponse(42L, "New", "New Original", null, 2001, 90, "en",
				null, null, 1L, List.of("Dir"), List.of(), List.of("drama"), List.of());
		when(movieService.create(any(MovieRequest.class))).thenReturn(created);
		String body = objectMapper.writeValueAsString(new MovieRequest("New", "New Original", null,
				2001, 90, "en", null, null, 1L, List.of("Dir"), List.of(), List.of("Drama"), List.of()));

		mockMvc.perform(post("/api/movies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/movies/42")))
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.directors[0]").value("Dir"))
				.andExpect(jsonPath("$.genres[0]").value("drama"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createInvalidBodyReturns400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/api/movies")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"\",\"releaseYear\":1500,\"runtimeMinutes\":0,\"originalLanguage\":\"EN\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("urn:problem:validation-error"))
				.andExpect(jsonPath("$.errors.title").exists())
				.andExpect(jsonPath("$.errors.originalTitle").exists())
				.andExpect(jsonPath("$.errors.releaseYear").exists())
				.andExpect(jsonPath("$.errors.runtimeMinutes").exists())
				.andExpect(jsonPath("$.errors.originalLanguage").exists())
				.andExpect(jsonPath("$.errors.tmdbId").exists());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createMalformedBodyReturns400() throws Exception {
		mockMvc.perform(post("/api/movies")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createDuplicateMovieMapsTo409() throws Exception {
		when(movieService.create(any(MovieRequest.class)))
				.thenThrow(new DuplicateMovieException("uq_movie_tmdb_id"));

		mockMvc.perform(post("/api/movies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new MovieRequest("T", "O", null, 2000,
								90, null, null, null, 1L, null, null, null, null))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value("urn:problem:duplicate-movie"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateReplacesMovie() throws Exception {
		MovieResponse updated = new MovieResponse(7L, "Updated", "Updated Original", null, 1990, 100,
				"fr", null, null, 1L, List.of(), List.of(), List.of(), List.of());
		when(movieService.update(eq(7L), any(MovieRequest.class))).thenReturn(updated);

		mockMvc.perform(put("/api/movies/7")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new MovieRequest("Updated", "Updated Original",
								null, 1990, 100, "fr", null, null, 1L, null, null, null, null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Updated"))
				.andExpect(jsonPath("$.directors").isEmpty());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateMissingMovieMapsTo404() throws Exception {
		when(movieService.update(eq(9L), any(MovieRequest.class)))
				.thenThrow(new NotFoundException("movie", 9L));

		mockMvc.perform(put("/api/movies/9")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new MovieRequest("T", "O", null, 2000,
								90, null, null, null, 1L, null, null, null, null))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("urn:problem:not-found"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void deleteReturns204() throws Exception {
		mockMvc.perform(delete("/api/movies/7"))
				.andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void deleteMissingMovieMapsTo404() throws Exception {
		org.mockito.Mockito.doThrow(new NotFoundException("movie", 8L)).when(movieService).delete(8L);

		mockMvc.perform(delete("/api/movies/8"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("urn:problem:not-found"));
	}

	@Test
	@WithMockUser
	void userRoleCannotCreate() throws Exception {
		mockMvc.perform(post("/api/movies")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser
	void userRoleCannotDelete() throws Exception {
		mockMvc.perform(delete("/api/movies/7"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithAnonymousUser
	void anonymousApiReadIsChallengedWithBasic() throws Exception {
		mockMvc.perform(get("/api/movies")
				.with(httpBasic("nobody", "wrong-password")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithAnonymousUser
	void anonymousApiWriteIsUnauthorized() throws Exception {
		mockMvc.perform(delete("/api/movies/7"))
				.andExpect(status().isUnauthorized());
	}

}
