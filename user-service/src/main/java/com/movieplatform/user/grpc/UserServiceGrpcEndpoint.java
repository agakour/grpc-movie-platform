package com.movieplatform.user.grpc;

import org.springframework.grpc.server.service.GrpcService;

import io.grpc.stub.StreamObserver;

import movieplatform.user.GetVoteRequest;
import movieplatform.user.GetVoteResponse;
import movieplatform.user.ListVotedMovieIdsRequest;
import movieplatform.user.ListVotedMovieIdsResponse;
import movieplatform.user.SetVoteRequest;
import movieplatform.user.SetVoteResponse;
import movieplatform.user.UserServiceGrpc;
import movieplatform.user.Vote;

import com.movieplatform.user.service.VoteService;

@GrpcService
@SuppressWarnings("unused")
public class UserServiceGrpcEndpoint extends UserServiceGrpc.UserServiceImplBase {

	private final VoteService voteService;

	public UserServiceGrpcEndpoint(VoteService voteService) {
		this.voteService = voteService;
	}

	@Override
	public void setVote(SetVoteRequest request, StreamObserver<SetVoteResponse> responseObserver) {
		voteService.setVote(request.getUserId(), request.getMovieId(), request.getLiked());
		responseObserver.onNext(SetVoteResponse.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void getVote(GetVoteRequest request, StreamObserver<GetVoteResponse> responseObserver) {
		var liked = voteService.getVote(request.getUserId(), request.getMovieId());
		responseObserver.onNext(GetVoteResponse.newBuilder()
				.setFound(liked.isPresent())
				.setLiked(liked.orElse(false))
				.build());
		responseObserver.onCompleted();
	}

	@Override
	public void listVotedMovieIds(ListVotedMovieIdsRequest request,
			StreamObserver<ListVotedMovieIdsResponse> responseObserver) {
		var votes = voteService.listVotes(request.getUserId()).stream()
				.map(vote -> Vote.newBuilder()
						.setMovieId(vote.movieId())
						.setLiked(vote.liked())
						.build())
				.toList();
		responseObserver.onNext(ListVotedMovieIdsResponse.newBuilder()
				.addAllVotes(votes)
				.build());
		responseObserver.onCompleted();
	}

}
