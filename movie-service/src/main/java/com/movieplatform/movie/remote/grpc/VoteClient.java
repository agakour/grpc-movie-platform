package com.movieplatform.movie.remote.grpc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.grpc.StatusRuntimeException;

import movieplatform.user.GetVoteRequest;
import movieplatform.user.GetVoteResponse;
import movieplatform.user.ListVotedMovieIdsRequest;
import movieplatform.user.ListVotedMovieIdsResponse;
import movieplatform.user.SetVoteRequest;
import movieplatform.user.SetVoteResponse;
import movieplatform.user.UserServiceGrpc;

@Component
@SuppressWarnings("unused")
public class VoteClient {

	static final String SERVICE = "user-service";

	private static final long CALL_DEADLINE_SECONDS = 3;

	private final UserServiceGrpc.UserServiceBlockingStub stub;

	public VoteClient(UserServiceGrpc.UserServiceBlockingStub stub) {
		this.stub = stub;
	}

	public void setVote(long userId, long movieId, boolean liked) {
		try {

			SetVoteResponse response = stub.withDeadlineAfter(CALL_DEADLINE_SECONDS, TimeUnit.SECONDS)
					.setVote(SetVoteRequest.newBuilder()
							.setUserId(userId)
							.setMovieId(movieId)
							.setLiked(liked)
							.build());
		}
		catch (StatusRuntimeException exception) {
			throw map(exception);
		}
	}

	public Optional<Boolean> getVote(long userId, long movieId) {
		try {
			GetVoteResponse response = stub.withDeadlineAfter(CALL_DEADLINE_SECONDS, TimeUnit.SECONDS)
					.getVote(GetVoteRequest.newBuilder()
							.setUserId(userId)
							.setMovieId(movieId)
							.build());
			return response.getFound() ? Optional.of(response.getLiked()) : Optional.empty();
		}
		catch (StatusRuntimeException exception) {
			throw map(exception);
		}
	}

	public List<VoteItem> listVotes(long userId) {
		try {
			ListVotedMovieIdsResponse response = stub.withDeadlineAfter(CALL_DEADLINE_SECONDS, TimeUnit.SECONDS)
					.listVotedMovieIds(ListVotedMovieIdsRequest.newBuilder()
							.setUserId(userId)
							.build());
			return response.getVotesList().stream()
					.map(vote -> new VoteItem(vote.getMovieId(), vote.getLiked()))
					.toList();
		}
		catch (StatusRuntimeException exception) {
			throw map(exception);
		}
	}

	private static RemoteGrpcException map(StatusRuntimeException exception) {
		return new RemoteGrpcException(SERVICE, exception.getStatus().getCode(), exception);
	}

}
