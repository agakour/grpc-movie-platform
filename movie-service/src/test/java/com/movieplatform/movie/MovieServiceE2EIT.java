package com.movieplatform.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.movieplatform.movie.catalog.dto.MovieRequest;
import com.movieplatform.movie.testutil.PostgresTestSupport;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unused")
class MovieServiceE2EIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestSupport.POSTGRES_IMAGE);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static final AtomicLong TMDB_ID_SEQ = new AtomicLong(900_000_000L);

	private static long uniqueTmdbId() {
		return TMDB_ID_SEQ.getAndIncrement();
	}

	@Test
	void browsePageIsPublic() throws Exception {
		MvcResult result = mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("browse"))
				.andReturn();
		String html = result.getResponse().getContentAsString();
		assertThat(html).contains("<title>gRPC Movie Platform</title>");
		assertThat(html).contains("rel=\"icon\"");
		assertThat(html).contains("/favicon.svg");
	}

	@Test
	void browseIgnoresBlankFiltersAndPaginationLinksOmitThem() throws Exception {
		MvcResult result = mockMvc.perform(get("/")
						.param("q", "   ")
						.param("genre", "")
						.param("keyword", "")
						.param("director", "")
						.param("actor", "  ")
						.param("year", "")
						.param("sortBy", "releaseYear")
						.param("dir", "desc")
						.param("page", "0")
						.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(view().name("browse"))
				.andReturn();
		String html = result.getResponse().getContentAsString();
		assertThat(html).doesNotContain("No movies match these filters.");
		assertThat(html).doesNotContain("?q=");
		assertThat(html).contains("sortBy=releaseYear");
		assertThat(html).contains("page=1");
	}

	@Test
	void browsePageBeyondLastPageRedirectsToLastPage() throws Exception {
		mockMvc.perform(get("/")
						.param("sortBy", "releaseYear")
						.param("dir", "desc")
						.param("page", "99")
						.param("size", "20"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location",
						"/?sortBy=releaseYear&dir=desc&page=66&size=20"));
	}

	@Test
	void browseDirectorSearchIsCaseInsensitiveAndTokenOrderIndependent() throws Exception {
		MvcResult result = mockMvc.perform(get("/")
						.param("director", "anderson wes")
						.param("page", "0")
						.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(view().name("browse"))
				.andReturn();
		String html = result.getResponse().getContentAsString();
		assertThat(html).contains("Rushmore");
		assertThat(html).contains("The Royal Tenenbaums");
		assertThat(html).contains("The Life Aquatic with Steve Zissou");
		assertThat(html).doesNotContain("No movies match these filters.");
	}

	@Test
	void movieDetailPageIsPublic() throws Exception {
		mockMvc.perform(get("/movies/1"))
				.andExpect(status().isOk())
				.andExpect(view().name("movie-detail"));
	}

	@Test
	void unknownMovieDetailRenders404Page() throws Exception {
		mockMvc.perform(get("/movies/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void missingArtworkRendersBrandedPlaceholderAndPresentArtworkDoesNot() throws Exception {
		String withArt = mockMvc.perform(get("/movies/1"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(withArt).contains("class=\"backdrop\"");
		assertThat(withArt).contains("class=\"poster\"");
		assertThat(withArt).doesNotContain("class=\"no-art\"");

		String suffix = String.valueOf(System.nanoTime());
		String body = objectMapper.writeValueAsString(new MovieRequest(
				"No Art " + suffix, "No Art Original " + suffix, null, 2004, 88, "en", null, null,
				uniqueTmdbId(), List.of("Dir"), List.of(), List.of("drama"), List.of()));
		MvcResult created = mockMvc.perform(post("/api/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

		String withoutArt = mockMvc.perform(get("/movies/{id}", id))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(withoutArt).contains("class=\"no-art\"");
		assertThat(withoutArt).doesNotContain("class=\"backdrop\"");
		assertThat(withoutArt).doesNotContain("class=\"poster\"");

		mockMvc.perform(delete("/api/movies/{id}", id)
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isNoContent());
	}

	@Test
	void anonymousVoteRedirectsToLogin() throws Exception {
		mockMvc.perform(post("/movies/1/vote").with(csrf()).param("liked", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
	}

	@Test
	void anonymousRecommendationsRedirectToLogin() throws Exception {
		mockMvc.perform(get("/recommendations"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void adminPagesRequireAdminRole() throws Exception {
		mockMvc.perform(get("/admin/movies"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));

		mockMvc.perform(get("/admin/movies").with(httpBasic("user", "change-me-user")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/admin/movies").with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movies"));
	}

	@Test
	void adminMoviesPageBeyondLastPageRedirectsToLastPage() throws Exception {
		mockMvc.perform(get("/admin/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.param("page", "99")
						.param("size", "20"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/admin/movies?page=66&size=20"));
	}

	@Test
	void adminMoviesBlankSearchIsOmittedFromPaginationLinks() throws Exception {
		MvcResult result = mockMvc.perform(get("/admin/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.param("q", "   ")
						.param("page", "0")
						.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/movies"))
				.andReturn();
		String html = result.getResponse().getContentAsString();
		assertThat(html).doesNotContain("?q=");
		assertThat(html).contains("page=1");
	}

	@Test
	void springdocScalarUiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/scalar"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));

		mockMvc.perform(get("/scalar").with(httpBasic("user", "change-me-user")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(get("/v3/api-docs").with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").value("3.1.0"));
	}

	@Test
	void apiReadsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/movies").param("size", "1"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/movies").param("size", "1")
						.with(httpBasic("user", "change-me-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1));

		mockMvc.perform(get("/api/movies").param("size", "1")
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isOk());
	}

	@Test
	void apiWritesRequireAdminRoleAndListIsSeeded() throws Exception {
		mockMvc.perform(get("/api/movies").param("size", "3")
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1340));

		String body = objectMapper.writeValueAsString(new MovieRequest(
				"Sec Probe " + System.nanoTime(), "Sec Original " + System.nanoTime(), null, 2008, 90,
				"en", null, null, uniqueTmdbId(), List.of("Sec Director"), List.of(), List.of("drama"), List.of()));

		mockMvc.perform(post("/api/movies")
						.with(httpBasic("user", "change-me-user"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());

		MvcResult created = mockMvc.perform(post("/api/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.directors[0]").value("Sec Director"))
				.andReturn();
		long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(get("/api/movies/{id}", id)
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));

		mockMvc.perform(delete("/api/movies/{id}", id)
						.with(httpBasic("user", "change-me-user")))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/movies/{id}", id)
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isNoContent());
	}

	@Test
	void catalogCrudAndBrowseSmoke() throws Exception {
		String suffix = String.valueOf(System.nanoTime());

		MvcResult listResult = mockMvc.perform(get("/api/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.param("size", "3")
						.param("sortBy", "releaseYear")
						.param("dir", "desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1340))
				.andExpect(jsonPath("$.content.length()").value(3))
				.andReturn();
		JsonNode listing = objectMapper.readTree(listResult.getResponse().getContentAsString());
		assertThat(listing.get("content").get(0).get("releaseYear").asInt())
				.isGreaterThanOrEqualTo(listing.get("content").get(2).get("releaseYear").asInt());

		String createBody = objectMapper.writeValueAsString(new MovieRequest(
				"E2E Probe " + suffix, "E2E Original " + suffix, null, 2005, 90, "en", null, null, uniqueTmdbId(),
				List.of(" E2E Director "), List.of("E2E Actor"), List.of("e2egenre", " DRAMA "), List.of("e2ekw")));
		MvcResult createdResult = mockMvc.perform(post("/api/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.title").value("E2E Probe " + suffix))
				.andExpect(jsonPath("$.directors[0]").value("E2E Director"))
				.andExpect(jsonPath("$.cast[0]").value("E2E Actor"))
				.andExpect(jsonPath("$.genres[0]").value("e2egenre"))
				.andExpect(jsonPath("$.genres[1]").value("drama"))
				.andExpect(jsonPath("$.keywords[0]").value("e2ekw"))
				.andReturn();
		long id = objectMapper.readTree(createdResult.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(get("/api/movies/{id}", id)
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.originalLanguage").value("en"))
				.andExpect(jsonPath("$.genres.length()").value(2));

		mockMvc.perform(post("/api/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value("urn:problem:duplicate-movie"));

		String updateBody = objectMapper.writeValueAsString(new MovieRequest(
				"E2E Probe Updated " + suffix, "E2E Original " + suffix, "overview", 2006, 91, "fr", null, null,
				uniqueTmdbId(), List.of(), List.of(), List.of(), List.of()));
		mockMvc.perform(put("/api/movies/{id}", id)
						.with(httpBasic("admin", "change-me-admin"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("E2E Probe Updated " + suffix))
				.andExpect(jsonPath("$.releaseYear").value(2006))
				.andExpect(jsonPath("$.originalLanguage").value("fr"))
				.andExpect(jsonPath("$.directors").isEmpty())
				.andExpect(jsonPath("$.genres").isEmpty());

		mockMvc.perform(delete("/api/movies/{id}", id)
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/movies/{id}", id)
						.with(httpBasic("admin", "change-me-admin")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("urn:problem:not-found"));
	}

	@Test
	void duplicateListEntriesReturn400() throws Exception {
		String suffix = String.valueOf(System.nanoTime());
		String body = objectMapper.writeValueAsString(new MovieRequest(
				"Dup List Probe " + suffix, "Dup List Original " + suffix, null, 2009, 92, "en", null, null,
				uniqueTmdbId(), List.of("Dup Director", " Dup Director "), List.of(), List.of("drama", " DRAMA "), List.of()));

		mockMvc.perform(post("/api/movies")
						.with(httpBasic("admin", "change-me-admin"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("urn:problem:invalid-request"));
	}

	@Test
	void invalidBodyReturnsValidationProblem() throws Exception {
		mockMvc.perform(post("/api/movies")
						.with(httpBasic("admin", "change-me-admin"))
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
	void registerThenLoginCreatesAndAuthenticatesSession() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(view().name("register"));

		String username = "newuser";
		mockMvc.perform(post("/register")
						.with(csrf())
						.param("username", username)
						.param("password", "change-me-registered"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(post("/register")
						.with(csrf())
						.param("username", username)
						.param("password", "change-me-registered-again"))
				.andExpect(status().isOk())
				.andExpect(view().name("register"));
	}

}
