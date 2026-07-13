package com.movieplatform.movie.remote.grpc;

import io.grpc.Status;

public class RemoteGrpcException extends RuntimeException {

	private final Status.Code code;

	public RemoteGrpcException(String service, Status.Code code, Throwable cause) {
		super("Call to " + service + " failed with gRPC status " + code, cause);
		this.code = code;
	}

	public Status.Code code() {
		return code;
	}

}
