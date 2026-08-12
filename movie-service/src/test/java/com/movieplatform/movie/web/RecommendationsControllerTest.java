package com.movieplatform.movie.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import com.movieplatform.movie.catalog.entity.Movie;
import com.movieplatform.movie.catalog.repository.MovieRepository;
import com.movieplatform.movie.remote.grpc.RecommendationClient;
import com.movieplatform.movie.remote.grpc.RecommendationItem;
import com.movieplatform.movie.remote.grpc.RemoteGrpcException;
import com.movieplatform.movie.remote.grpc.VoteClient;
import com.movieplatform.movie.remote.grpc.VoteItem;
import com.movieplatform.movie.security.session.CurrentUser;
import com.movieplatform.movie.security.session.MoviePlatformUser;
import com.movieplatform.movie.util.TmdbImageUrl;

import io.grpc.Status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class RecommendationsControllerTest {

	@Mock
	private VoteClient voteClient;
	@Mock
	private RecommendationClient recommendationClient;
	@Mock
	private MovieRepository movieRepository;
	@Mock
	private CurrentUser currentUser;

	private RecommendationsController controller;

	@BeforeEach
	void setUp() {
		controller = new RecommendationsController(voteClient, recommendationClient,
				movieRepository, currentUser, new TmdbImageUrl());
	}

	private static Movie movie(long id, Long tmdbId) {
		Movie movie = new Movie();
		movie.setId(id);
		movie.setTitle("M" + id);
		movie.setOriginalTitle("M" + id);
		movie.setTmdbId(tmdbId);
		return movie;
	}

	@Test
	void likedMoviesResolveToTmdbIdsAndOnlyCleanCatalogItemsSurvive() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(
				new VoteItem(101L, true),
				new VoteItem(104L, false)));
		Movie likedCatalog = movie(101L, 550L);
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(likedCatalog));
		when(recommendationClient.recommendations(List.of(550L))).thenReturn(List.of(
				new RecommendationItem(551L, "Fight Club", 1999, "/fc.jpg", "Overview", 1.5),
				new RecommendationItem(999L, "Not in catalog", 2000, "/x.jpg", "", 0.9),
				new RecommendationItem(552L, "Already voted", 2001, "/y.jpg", "", 2.0)));

		when(movieRepository.findByTmdbIdIn(List.of(551L, 999L, 552L)))
				.thenReturn(List.of(movie(103L, 551L), movie(104L, 552L)));

		ExtendedModelMap model = new ExtendedModelMap();
		String view = controller.recommendations(model);

		assertThat(view).isEqualTo("recommendations");
		assertThat(model).doesNotContainKey("errorMessage");
		assertThat(model).doesNotContainKey("noLikedMovies");
		@SuppressWarnings("unchecked")
		List<RecommendationsController.RecommendationView> views =
				(List<RecommendationsController.RecommendationView>) model.get("recommendations");
		assertThat(views).hasSize(1);
		RecommendationsController.RecommendationView viewItem = views.getFirst();
		assertThat(viewItem.movieId()).isEqualTo(103L);
		assertThat(viewItem.tmdbId()).isEqualTo(551L);
		assertThat(viewItem.title()).isEqualTo("Fight Club");
		assertThat(viewItem.score()).isEqualTo(150);
	}

	@Test
	void scoreIsRoundedToIntegerPercent() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(new VoteItem(101L, true)));
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(movie(101L, 550L)));
		when(recommendationClient.recommendations(List.of(550L))).thenReturn(List.of(
				new RecommendationItem(551L, "A", 1990, "/a.jpg", "", 1.5),
				new RecommendationItem(553L, "B", 1991, "/b.jpg", "", 4.0 / 3.0),
				new RecommendationItem(554L, "C", 1992, "/c.jpg", "", 0.5)));
		when(movieRepository.findByTmdbIdIn(List.of(551L, 553L, 554L)))
				.thenReturn(List.of(movie(103L, 551L), movie(105L, 553L), movie(106L, 554L)));

		ExtendedModelMap model = new ExtendedModelMap();
		controller.recommendations(model);

		@SuppressWarnings("unchecked")
		List<RecommendationsController.RecommendationView> views =
				(List<RecommendationsController.RecommendationView>) model.get("recommendations");
		assertThat(views).hasSize(3);
		assertThat(views).extracting(RecommendationsController.RecommendationView::score)
				.containsExactly(150, 133, 50);
	}

	@Test
	void viewsAreCappedAtDisplayLimitAfterCatalogFiltering() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(new VoteItem(101L, true)));
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(movie(101L, 550L)));
		List<RecommendationItem> items = new ArrayList<>();
		List<Long> tmdbIds = new ArrayList<>();
		for (int i = 0; i < 22; i++) {
			items.add(new RecommendationItem(600L + i, "Movie " + i, 1990 + i, "/p.jpg", "", 1.0 + i));
			tmdbIds.add(600L + i);
		}
		when(recommendationClient.recommendations(List.of(550L))).thenReturn(items);
		when(movieRepository.findByTmdbIdIn(tmdbIds))
				.thenReturn(tmdbIds.stream().map(id -> movie(200L + id, id)).toList());

		ExtendedModelMap model = new ExtendedModelMap();
		controller.recommendations(model);

		@SuppressWarnings("unchecked")
		List<RecommendationsController.RecommendationView> views =
				(List<RecommendationsController.RecommendationView>) model.get("recommendations");
		assertThat(views).hasSize(20);
	}

	@Test
	void noLikedMoviesShowsEmptyStateWithoutCallingRecommendations() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(new VoteItem(102L, false)));

		ExtendedModelMap model = new ExtendedModelMap();
		String view = controller.recommendations(model);

		assertThat(view).isEqualTo("recommendations");
		assertThat(model.get("noLikedMovies")).isEqualTo(true);
		verify(recommendationClient, never()).recommendations(anyList());
	}

	@Test
	void likedMoviesWithoutResolvedTmdbIdYieldEmptyState() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(new VoteItem(101L, true)));
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(movie(101L, null)));

		ExtendedModelMap model = new ExtendedModelMap();
		controller.recommendations(model);

		assertThat(model.get("noLikedMovies")).isEqualTo(true);
		verify(recommendationClient, never()).recommendations(anyList());
	}

	@Test
	void emptyRecommendationStreamShowsEmptyStateInsteadOfBlankPage() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(new VoteItem(101L, true)));
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(movie(101L, 550L)));
		when(recommendationClient.recommendations(List.of(550L))).thenReturn(List.of());
		when(movieRepository.findByTmdbIdIn(anyList())).thenReturn(List.of());

		ExtendedModelMap model = new ExtendedModelMap();
		String view = controller.recommendations(model);

		assertThat(view).isEqualTo("recommendations");
		assertThat(model.get("noRecommendations")).isEqualTo(true);
		assertThat(model).doesNotContainKey("errorMessage");
		assertThat(model).doesNotContainKey("recommendations");
	}

	@Test
	void recommendationFailureMapsToFriendlyMessage() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(new VoteItem(101L, true)));
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(movie(101L, 550L)));
		when(recommendationClient.recommendations(List.of(550L)))
				.thenThrow(new RemoteGrpcException("recommendation-service", Status.Code.RESOURCE_EXHAUSTED, null));

		ExtendedModelMap model = new ExtendedModelMap();
		String view = controller.recommendations(model);

		assertThat(view).isEqualTo("recommendations");
		assertThat(model.get("errorMessage")).isEqualTo(
				"TMDb rate limit exceeded — please try again in a moment.");
	}

	@Test
	void voteStoreFailureMapsToFriendlyMessage() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenThrow(
				new RemoteGrpcException("user-service", Status.Code.UNAVAILABLE, null));

		ExtendedModelMap model = new ExtendedModelMap();
		String view = controller.recommendations(model);

		assertThat(view).isEqualTo("recommendations");
		assertThat(model.get("errorMessage")).isEqualTo(
				"The recommendation service is currently unavailable.");
	}

	@Test
	void alreadyVotedCatalogItemIsExcluded() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(new VoteItem(101L, true)));
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(movie(101L, 550L)));
		when(recommendationClient.recommendations(List.of(550L))).thenReturn(List.of(
				new RecommendationItem(550L, "Rec of liked itself", 1990, "/x.jpg", "", 0.5)));

		when(movieRepository.findByTmdbIdIn(List.of(550L))).thenReturn(List.of(movie(101L, 550L)));

		ExtendedModelMap model = new ExtendedModelMap();
		controller.recommendations(model);

		assertThat(model.get("noRecommendations")).isEqualTo(true);
		assertThat(model).doesNotContainKey("recommendations");
	}

	@Test
	void allRecommendationsFilteredOutShowsEmptyState() {
		when(currentUser.require()).thenReturn(new MoviePlatformUser(7L, "user", "x", "USER"));
		when(voteClient.listVotes(7L)).thenReturn(List.of(
				new VoteItem(101L, true),
				new VoteItem(104L, false)));
		when(movieRepository.findAllById(List.of(101L))).thenReturn(List.of(movie(101L, 550L)));
		when(recommendationClient.recommendations(List.of(550L))).thenReturn(List.of(
				new RecommendationItem(999L, "Not in catalog", 2000, "/x.jpg", "", 0.9),
				new RecommendationItem(552L, "Already voted", 2001, "/y.jpg", "", 2.0)));

		when(movieRepository.findByTmdbIdIn(List.of(999L, 552L)))
				.thenReturn(List.of(movie(104L, 552L)));

		ExtendedModelMap model = new ExtendedModelMap();
		String view = controller.recommendations(model);

		assertThat(view).isEqualTo("recommendations");
		assertThat(model.get("noRecommendations")).isEqualTo(true);
		assertThat(model).doesNotContainKey("errorMessage");
		assertThat(model).doesNotContainKey("recommendations");
	}

}
