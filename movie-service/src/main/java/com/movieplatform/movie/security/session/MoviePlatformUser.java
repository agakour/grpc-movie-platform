package com.movieplatform.movie.security.session;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MoviePlatformUser implements UserDetails {

	private final Long id;

	private final String username;

	private final String passwordHash;

	private final String role;

	public MoviePlatformUser(Long id, String username, String passwordHash, String role) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	@Override
	public @NonNull String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
