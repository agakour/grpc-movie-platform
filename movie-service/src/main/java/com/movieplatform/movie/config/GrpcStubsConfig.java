package com.movieplatform.movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import movieplatform.recommendation.RecommendationServiceGrpc;
import movieplatform.user.UserServiceGrpc;

@Configuration
@SuppressWarnings("unused")
public class GrpcStubsConfig {

	@Bean
	public UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub(GrpcChannelFactory channelFactory) {
		return UserServiceGrpc.newBlockingStub(channelFactory.createChannel("user-service"));
	}

	@Bean
	public RecommendationServiceGrpc.RecommendationServiceBlockingStub recommendationServiceBlockingStub(
			GrpcChannelFactory channelFactory) {
		return RecommendationServiceGrpc.newBlockingStub(channelFactory.createChannel("recommendation-service"));
	}

}
