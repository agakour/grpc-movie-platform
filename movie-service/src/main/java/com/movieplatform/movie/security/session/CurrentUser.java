package com.movieplatform.movie.security.session;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

	public MoviePlatformUser require() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof MoviePlatformUser principal)) {
			throw new IllegalStateException("No authenticated movie-platform user in this security context");
		}
		return principal;
	}

	public Optional<MoviePlatformUser> optional() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof MoviePlatformUser principal)) {
			return Optional.empty();
		}
		return Optional.of(principal);
	}

}
