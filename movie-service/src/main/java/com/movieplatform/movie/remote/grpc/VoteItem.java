package com.movieplatform.movie.remote.grpc;

public record VoteItem(long movieId, boolean liked) {
}
