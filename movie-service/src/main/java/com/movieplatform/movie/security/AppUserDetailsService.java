package com.movieplatform.movie.security;

import com.movieplatform.movie.security.session.MoviePlatformUser;
import com.movieplatform.movie.security.user.AppUser;
import com.movieplatform.movie.security.user.AppUserRepository;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("unused")
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	public AppUserDetailsService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Override
	public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
		AppUser user = appUserRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("No user named '" + username + "'"));
		return new MoviePlatformUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole());
	}

}
