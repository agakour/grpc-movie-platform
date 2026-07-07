package com.movieplatform.movie.security;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiFirstAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final RequestMatcher API_MATCHER = PathPatternRequestMatcher.pathPattern("/api/**");

	private final AuthenticationEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

	private final AuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");

	@Override
	public void commence(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull AuthenticationException authException) throws IOException, ServletException {
		if (API_MATCHER.matches(request)) {
			unauthorized.commence(request, response, authException);
		}
		else {
			login.commence(request, response, authException);
		}
	}

}