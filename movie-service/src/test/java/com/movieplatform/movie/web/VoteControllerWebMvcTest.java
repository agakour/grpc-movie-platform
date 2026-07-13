package com.movieplatform.movie.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.movieplatform.movie.catalog.repository.MovieRepository;
import com.movieplatform.movie.remote.grpc.VoteClient;
import com.movieplatform.movie.security.AppUserDetailsService;
import com.movieplatform.movie.security.SecurityConfig;
import com.movieplatform.movie.security.session.CurrentUser;
import com.movieplatform.movie.security.session.MoviePlatformUser;
import com.movieplatform.movie.testutil.MovieMvcSecurityTestCustomizer;

@WebMvcTest(controllers = VoteController.class)
@Import({SecurityConfig.class,
		VoteControllerWebMvcTest.SecurityMockMvcConfig.class,
		SecurityAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class})
@SuppressWarnings("unused")
class VoteControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VoteClient voteClient;

	@MockitoBean
	private MovieRepository movieRepository;

	@MockitoBean
	private CurrentUser currentUser;

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
	@WithMockUser
	void voteCallsSetVoteWithSessionUserIdAndRedirectsBack() throws Exception {
		when(movieRepository.existsById(5L)).thenReturn(true);
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));

		mockMvc.perform(post("/movies/5/vote").with(csrf()).param("liked", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/movies/5"));

		verify(voteClient).setVote(7L, 5L, true);
	}

	@Test
	@WithMockUser
	void dislikeIsForwarded() throws Exception {
		when(movieRepository.existsById(5L)).thenReturn(true);
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));

		mockMvc.perform(post("/movies/5/vote").with(csrf()).param("liked", "false"))
				.andExpect(status().is3xxRedirection());

		verify(voteClient).setVote(7L, 5L, false);
	}

	@Test
	@WithMockUser
	void unknownMovieReturns404WithoutVoteCall() throws Exception {
		when(movieRepository.existsById(999L)).thenReturn(false);

		mockMvc.perform(post("/movies/999/vote").with(csrf()).param("liked", "true"))
				.andExpect(status().isNotFound());

		verifyNoInteractions(voteClient);
	}

	@Test
	@WithAnonymousUser
	void anonymousVoteRedirectsToLogin() throws Exception {
		mockMvc.perform(post("/movies/5/vote").with(csrf()).param("liked", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));

		verify(voteClient, never()).setVote(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
	}

}
