package com.movieplatform.user.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import movieplatform.user.GetVoteRequest;
import movieplatform.user.GetVoteResponse;
import movieplatform.user.ListVotedMovieIdsRequest;
import movieplatform.user.ListVotedMovieIdsResponse;
import movieplatform.user.SetVoteRequest;
import movieplatform.user.SetVoteResponse;
import movieplatform.user.UserServiceGrpc;
import movieplatform.user.Vote;

import com.movieplatform.user.testutil.PostgresTestSupport;

@Testcontainers
@SpringBootTest(properties = "spring.grpc.server.port=0")
@SuppressWarnings("unused")
class UserServiceGrpcEndpointIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer POSTGRES = new PostgreSQLContainer(PostgresTestSupport.POSTGRES_IMAGE);

	@Autowired
	private GrpcServerLifecycle grpcServerLifecycle;

	private ManagedChannel channel;

	private UserServiceGrpc.UserServiceBlockingStub stub;

	@BeforeEach
	void connect() {
		channel = ManagedChannelBuilder.forAddress("127.0.0.1", grpcServerLifecycle.getPort())
				.usePlaintext()
				.build();
		stub = UserServiceGrpc.newBlockingStub(channel);
	}

	@AfterEach
	void disconnect() throws InterruptedException {
		channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
	}

	@Test
	void setGetAndListVotesOverRealGrpcServer() {
		long userId = 42L;

		SetVoteResponse likedVote = stub.setVote(
				SetVoteRequest.newBuilder().setUserId(userId).setMovieId(101L).setLiked(true).build());
		assertThat(likedVote).isNotNull();
		SetVoteResponse dislikedVote = stub.setVote(
				SetVoteRequest.newBuilder().setUserId(userId).setMovieId(102L).setLiked(false).build());
		assertThat(dislikedVote).isNotNull();

		GetVoteResponse liked = stub.getVote(GetVoteRequest.newBuilder()
				.setUserId(userId)
				.setMovieId(101L)
				.build());
		assertThat(liked.getFound()).isTrue();
		assertThat(liked.getLiked()).isTrue();

		GetVoteResponse missing = stub.getVote(GetVoteRequest.newBuilder()
				.setUserId(userId)
				.setMovieId(999L)
				.build());
		assertThat(missing.getFound()).isFalse();

		SetVoteResponse updatedVote = stub.setVote(
				SetVoteRequest.newBuilder().setUserId(userId).setMovieId(101L).setLiked(false).build());
		assertThat(updatedVote).isNotNull();
		ListVotedMovieIdsResponse votes = stub.listVotedMovieIds(ListVotedMovieIdsRequest.newBuilder()
				.setUserId(userId)
				.build());
		assertThat(votes.getVotesList())
				.extracting(Vote::getMovieId, Vote::getLiked)
				.containsExactly(tuple(101L, false), tuple(102L, false));
	}

}
